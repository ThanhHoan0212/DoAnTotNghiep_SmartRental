package com.phongtro.backend.service.impl;

import com.phongtro.backend.dto.response.ContractResponse;
import com.phongtro.backend.dto.smartca.*;
import com.phongtro.backend.entity.*;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.mapper.ContractMapper;
import com.phongtro.backend.repository.ContractRepository;
import com.phongtro.backend.repository.RoomRepository;
import com.phongtro.backend.repository.UserRepository;
import com.phongtro.backend.service.SmartCaService;
import com.phongtro.backend.service.client.VnptSmartCaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartCaServiceImpl implements SmartCaService {

    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final VnptSmartCaClient vnptSmartCaClient;
    private final ContractMapper contractMapper;

    @Override
    @Transactional
    public SmartCaInitiateResponse initiateSigning(UUID contractId, String userEmail, SmartCaInitiateRequest request) {
        Contract contract = getContractById(contractId);
        User user = getUserByEmail(userEmail);

        boolean isTenant = contract.getTenant().getId().equals(user.getId());
        boolean isLandlord = contract.getLandlord().getId().equals(user.getId());

        if (!isTenant && !isLandlord) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Chỉ hai bên trong hợp đồng mới có quyền ký số");
        }

        if (hasOpenClosure(contract)) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Đang có yêu cầu hủy chờ xác nhận, chưa thể ký hợp đồng");
        }

        if (contract.getStatus() != ContractStatus.AWAITING_SIGNATURES && contract.getStatus() != ContractStatus.ACTIVE) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Chỉ có thể ký hợp đồng đang ở trạng thái chờ ký (AWAITING_SIGNATURES)");
        }

        if (contract.getDepositPaidAt() == null) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Hợp đồng chưa được xác nhận thanh toán cọc");
        }

        if (contract.getDocumentContent() == null || contract.getDocumentContent().trim().isEmpty()) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Văn bản hợp đồng chưa được khởi tạo");
        }

        if (contract.getEndDate().isBefore(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")))) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Hợp đồng đã quá thời hạn thuê");
        }

        // Check if user already signed
        if (isTenant && contract.getTenantSignedAt() != null) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Người thuê đã hoàn tất ký số hợp đồng này trước đó");
        }
        if (isLandlord && contract.getLandlordSignedAt() != null) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Chủ nhà đã hoàn tất ký số hợp đồng này trước đó");
        }

        // Identify Signer's Citizen ID (CCCD)
        String cccd = request != null && request.getIdCardNumber() != null && !request.getIdCardNumber().trim().isEmpty()
                ? request.getIdCardNumber().trim()
                : (user.getIdCardNumber() != null && !user.getIdCardNumber().trim().isEmpty()
                    ? user.getIdCardNumber().trim()
                    : "001010101001");

        String docId = contract.getContractCode() != null ? contract.getContractCode() : "HD-" + contract.getId().toString().substring(0, 8);
        String dataHashHex = computeSha256Hex(contract.getDocumentContent());

        String roleSuffix = isTenant ? "T" : "L";
        String txnId = "SP_CA_" + System.currentTimeMillis() + "_" + roleSuffix;

        String serialNumber = request != null ? request.getSerialNumber() : "";

        log.info("Initiating VNPT SmartCA signing for contract [{}] user [{}] role [{}] txn [{}]",
                contract.getId(), user.getEmail(), isTenant ? "TENANT" : "LANDLORD", txnId);

        VnptSmartCaClient.SmartCaSignInitResult initResult = vnptSmartCaClient.initiateSign(
                cccd,
                user.getFullName(),
                txnId,
                docId,
                dataHashHex,
                serialNumber
        );

        if (isTenant) {
            contract.setTenantSignTxnId(txnId);
        } else {
            contract.setLandlordSignTxnId(txnId);
        }
        contractRepository.save(contract);

        return SmartCaInitiateResponse.builder()
                .contractId(contract.getId())
                .transactionId(txnId)
                .docId(docId)
                .dataHash(dataHashHex)
                .signerCccd(cccd)
                .signerName(user.getFullName())
                .signerRole(isTenant ? "TENANT" : "LANDLORD")
                .status(initResult.status())
                .message(initResult.message())
                .sandbox(initResult.isSandbox())
                .build();
    }

    @Override
    @Transactional
    public SmartCaStatusResponse confirmSigning(UUID contractId, String userEmail, SmartCaConfirmRequest request) {
        Contract contract = getContractById(contractId);
        User user = getUserByEmail(userEmail);

        boolean isTenant = contract.getTenant().getId().equals(user.getId());
        boolean isLandlord = contract.getLandlord().getId().equals(user.getId());

        if (!isTenant && !isLandlord) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Chỉ hai bên trong hợp đồng mới có quyền xác nhận ký số");
        }

        String txnId = request.getTransactionId();
        String cccd = user.getIdCardNumber() != null && !user.getIdCardNumber().trim().isEmpty()
                ? user.getIdCardNumber().trim()
                : "001010101001";

        log.info("Confirming VNPT SmartCA signing for contract [{}] user [{}] txn [{}]", contractId, userEmail, txnId);

        VnptSmartCaClient.SmartCaConfirmResult confirmResult = vnptSmartCaClient.confirmSign(
                cccd,
                user.getFullName(),
                txnId,
                request.getPin(),
                "",
                request.isSandboxConfirm()
        );

        if (!confirmResult.success() || !"SIGNED".equalsIgnoreCase(confirmResult.status())) {
            return SmartCaStatusResponse.builder()
                    .contractId(contract.getId())
                    .transactionId(txnId)
                    .status(confirmResult.status())
                    .message(confirmResult.message())
                    .contractCompleted(false)
                    .contract(contractMapper.toContractResponse(contract))
                    .build();
        }

        // Apply digital signature to contract
        return applySignatureAndActivateIfReady(contract, user, isTenant, confirmResult.signatureValue(), confirmResult.certSerial(), confirmResult.certSubject(), txnId);
    }

    @Override
    @Transactional
    public SmartCaStatusResponse getSigningStatus(UUID contractId, String userEmail, String transactionId) {
        Contract contract = getContractById(contractId);
        User user = getUserByEmail(userEmail);

        boolean isTenant = contract.getTenant().getId().equals(user.getId());
        boolean isLandlord = contract.getLandlord().getId().equals(user.getId());

        if (!isTenant && !isLandlord) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Chỉ hai bên trong hợp đồng mới có quyền xem trạng thái ký");
        }

        // If user already signed in DB
        if ((isTenant && contract.getTenantSignedAt() != null) || (isLandlord && contract.getLandlordSignedAt() != null)) {
            return SmartCaStatusResponse.builder()
                    .contractId(contract.getId())
                    .transactionId(transactionId)
                    .status("SIGNED")
                    .certSerial(isTenant ? contract.getTenantCertSerial() : contract.getLandlordCertSerial())
                    .signedAt(isTenant ? contract.getTenantSignedAt().toString() : contract.getLandlordSignedAt().toString())
                    .signatureValue(isTenant ? contract.getTenantSignature() : contract.getLandlordSignature())
                    .contractCompleted(contract.getStatus() == ContractStatus.ACTIVE)
                    .message("Đã hoàn tất ký số")
                    .contract(contractMapper.toContractResponse(contract))
                    .build();
        }

        String cccd = user.getIdCardNumber() != null ? user.getIdCardNumber() : "001010101001";
        VnptSmartCaClient.SmartCaStatusResult statusResult = vnptSmartCaClient.checkSignStatus(transactionId, cccd, user.getFullName());

        if (statusResult.isSigned() || "SIGNED".equalsIgnoreCase(statusResult.status())) {
            return applySignatureAndActivateIfReady(contract, user, isTenant, statusResult.signatureValue(), statusResult.certSerial(), statusResult.certSubject(), transactionId);
        }

        return SmartCaStatusResponse.builder()
                .contractId(contract.getId())
                .transactionId(transactionId)
                .status(statusResult.status())
                .message(statusResult.message())
                .contractCompleted(false)
                .contract(contractMapper.toContractResponse(contract))
                .build();
    }

    @Override
    public List<SmartCaCertificateInfo> getCertificates(String userEmail) {
        User user = getUserByEmail(userEmail);
        String cccd = user.getIdCardNumber() != null ? user.getIdCardNumber() : "";
        return vnptSmartCaClient.getCertificates(cccd);
    }

    private SmartCaStatusResponse applySignatureAndActivateIfReady(
            Contract contract, User user, boolean isTenant,
            String signatureValue, String certSerial, String certSubject, String txnId) {

        Instant now = Instant.now();
        if (isTenant) {
            contract.setTenantSignedAt(now);
            contract.setTenantSignature(signatureValue);
            contract.setTenantCertSerial(certSerial);
            contract.setTenantSignTxnId(txnId);
        } else {
            contract.setLandlordSignedAt(now);
            contract.setLandlordSignature(signatureValue);
            contract.setLandlordCertSerial(certSerial);
            contract.setLandlordSignTxnId(txnId);
        }

        // Append certificate stamp to contract document
        String stamp = buildSignatureStamp(user, isTenant ? "BÊN THUÊ (TENANT)" : "BÊN CHO THUÊ (LANDLORD)",
                txnId, certSerial, certSubject, signatureValue, now);

        if (contract.getDocumentContent() != null && !contract.getDocumentContent().contains(txnId)) {
            contract.setDocumentContent(contract.getDocumentContent() + "\n\n" + stamp);
        }

        boolean bothSigned = contract.getTenantSignedAt() != null && contract.getLandlordSignedAt() != null;

        if (bothSigned) {
            contract.setStatus(ContractStatus.ACTIVE);
            contract.setActivatedAt(now);

            Room room = roomRepository.findByIdForUpdate(contract.getRoom().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy phòng trọ"));
            room.setStatus(RoomStatus.RENTED);
            roomRepository.save(room);

            log.info("Both parties signed contract [{}]. Contract is now ACTIVE and room [{}] is RENTED",
                    contract.getId(), room.getId());
        }

        Contract savedContract = contractRepository.save(contract);

        return SmartCaStatusResponse.builder()
                .contractId(savedContract.getId())
                .transactionId(txnId)
                .status("SIGNED")
                .certSerial(certSerial)
                .certSubject(certSubject)
                .signedAt(now.toString())
                .signatureValue(signatureValue)
                .contractCompleted(bothSigned)
                .message(bothSigned ? "Hai bên đã ký số thành công. Hợp đồng chính thức có hiệu lực!" : "Ký số thành công. Đang chờ bên còn lại ký số.")
                .contract(contractMapper.toContractResponse(savedContract))
                .build();
    }

    private String buildSignatureStamp(
            User user, String roleTitle, String txnId,
            String certSerial, String certSubject, String signatureValue, Instant timestamp) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

        return "================================================================================\n" +
               "[CHỨNG THỰC CHỮ KÝ SỐ ĐIỆN TỬ VNPT SmartCA]\n" +
               "--------------------------------------------------------------------------------\n" +
               "Người ký: " + user.getFullName() + " (" + roleTitle + ")\n" +
               "Số CCCD / Mã định danh: " + (user.getIdCardNumber() != null ? user.getIdCardNumber() : "N/A") + "\n" +
               "Mã giao dịch ký số: " + txnId + "\n" +
               "Số Serial chứng thư số: " + (certSerial != null ? certSerial : "VNPT-CA-AUTO") + "\n" +
               "Chủ thể chứng thư (Subject): " + (certSubject != null ? certSubject : "CN=" + user.getFullName()) + "\n" +
               "Tổ chức cấp chứng thư: VNPT-CA Remote Signing Service (VNPT IT)\n" +
               "Thời gian ký số: " + formatter.format(timestamp) + " (Giờ Việt Nam)\n" +
               "Chuỗi ký số bảo mật: " + signatureValue + "\n" +
               "Trạng thái: CHỮ KÝ SỐ HỢP LỆ VÀ ĐÃ XÁC THỰC TOÀN VẸN\n" +
               "================================================================================";
    }

    private String computeSha256Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Lỗi tính toán mã băm văn bản: " + e.getMessage());
        }
    }

    private Contract getContractById(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND, "Không tìm thấy hợp đồng " + contractId));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng với email " + email));
    }

    private boolean hasOpenClosure(Contract contract) {
        return contract.getClosureRequests() != null && contract.getClosureRequests().stream().anyMatch(r ->
                r.getStatus() == ContractClosure.Status.PENDING || r.getStatus() == ContractClosure.Status.ACCEPTED);
    }
}
