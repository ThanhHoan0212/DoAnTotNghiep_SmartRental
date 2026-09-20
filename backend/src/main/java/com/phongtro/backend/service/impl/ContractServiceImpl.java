package com.phongtro.backend.service.impl;

import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.request.CreateContractRequest;
import com.phongtro.backend.dto.request.CreateClosureRequest;
import com.phongtro.backend.dto.request.RespondClosureRequest;
import com.phongtro.backend.dto.request.UpdateContractStatusRequest;
import com.phongtro.backend.dto.response.ContractResponse;
import com.phongtro.backend.entity.*;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.mapper.ContractMapper;
import com.phongtro.backend.repository.ContractRepository;
import com.phongtro.backend.repository.RoomRepository;
import com.phongtro.backend.repository.UserRepository;
import com.phongtro.backend.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ContractMapper contractMapper;

    @Override
    @Transactional
    public ContractResponse createContractRequest(CreateContractRequest request, String tenantEmail) {
        User tenant = getUserByEmail(tenantEmail);

        // Bắt buộc người thuê phải hoàn tất định danh eKYC trước khi gửi yêu cầu thuê / cọc phòng (trừ ADMIN)
        if (!tenant.isIdentityVerified() && tenant.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.EKYC_REQUIRED, "Bạn cần hoàn tất xác thực danh tính điện tử eKYC (độ tin cậy > 85%) trước khi gửi yêu cầu thuê phòng & đặt cọc.");
        }

        Room room = roomRepository.findByIdForUpdate(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy phòng trọ"));

        // 1. Kiểm tra trạng thái phòng: chỉ phòng APPROVED mới được phép thuê
        if (room.getStatus() == RoomStatus.RENTED) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Phòng trọ này hiện đã có người thuê");
        }
        if (room.getStatus() != RoomStatus.APPROVED) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Phòng trọ hiện chưa sẵn sàng để đặt thuê");
        }

        // 2. Không cho phép chủ nhà tự gửi yêu cầu thuê phòng của chính mình
        if (room.getLandlord().getId().equals(tenant.getId())) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Bạn không thể gửi yêu cầu thuê phòng do chính mình đăng tin");
        }

        // 3. Kiểm tra ngày bắt đầu và kết thúc
        if (request.getEndDate().isBefore(request.getStartDate()) || request.getEndDate().isEqual(request.getStartDate())) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Ngày kết thúc hợp đồng phải sau ngày bắt đầu thuê");
        }

        // 4. Kiểm tra người thuê đã có yêu cầu PENDING hoặc ACTIVE cho phòng này chưa
        boolean hasPendingOrActive = contractRepository.existsByTenantAndRoomAndStatusIn(
                tenant, room, List.of(ContractStatus.PENDING, ContractStatus.AWAITING_DEPOSIT, ContractStatus.AWAITING_SIGNATURES, ContractStatus.ACTIVE)
        );
        if (hasPendingOrActive) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Bạn đã có một yêu cầu thuê hoặc hợp đồng đang hiệu lực cho phòng này");
        }

        // 5. Kiểm tra phòng đã có hợp đồng ACTIVE nào khác chưa
        boolean isRented = contractRepository.existsByRoomAndStatus(room, ContractStatus.ACTIVE);
        if (isRented) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Phòng trọ này đã được ký hợp đồng cho người khác");
        }

        // 6. Sinh mã hợp đồng tự động (VD: HD-202609-A1B2C3)
        String datePrefix = today().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String contractCode = "YC-" + datePrefix + "-" + randomSuffix;

        // 7. Số tiền cọc: nếu không nhập thì mặc định bằng 1 tháng tiền phòng
        if (request.getDepositAmount() != null &&
                (!Double.isFinite(request.getDepositAmount()) || request.getDepositAmount() < 0)) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Tiền cọc phải là số không âm hợp lệ");
        }
        Double deposit = request.getDepositAmount() != null
                ? request.getDepositAmount()
                : room.getPrice();

        Contract contract = Contract.builder()
                .contractCode(contractCode)
                .requestCode(contractCode)
                .tenant(tenant)
                .landlord(room.getLandlord())
                .room(room)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .monthlyRent(room.getPrice())
                .depositAmount(deposit)
                .status(ContractStatus.PENDING)
                .terms(request.getTerms())
                .build();

        Contract saved = contractRepository.save(contract);
        log.info("Tenant [{}] created rental contract request [{}] for room [{}]", tenantEmail, contractCode, room.getId());

        return contractMapper.toContractResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getContractById(UUID contractId, String userEmail) {
        Contract contract = getContractEntityById(contractId);
        User user = getUserByEmail(userEmail);

        boolean isTenant = contract.getTenant().getId().equals(user.getId());
        boolean isLandlord = contract.getLandlord().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isTenant && !isLandlord && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền xem thông tin hợp đồng này");
        }

        return contractMapper.toContractResponse(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContractResponse> getTenantContracts(String tenantEmail, ContractStatus status, int page, int size) {
        User tenant = getUserByEmail(tenantEmail);
        Pageable pageable = PageRequest.of(Math.max(0, page), size > 0 ? size : 10);

        Page<Contract> contracts = (status != null)
                ? contractRepository.findByTenantAndStatusOrderByCreatedAtDesc(tenant, status, pageable)
                : contractRepository.findByTenantOrderByCreatedAtDesc(tenant, pageable);

        return PageResponse.from(contracts.map(contractMapper::toContractResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContractResponse> getLandlordContracts(String landlordEmail, ContractStatus status, UUID roomId, int page, int size) {
        User landlord = getUserByEmail(landlordEmail);
        Pageable pageable = PageRequest.of(Math.max(0, page), size > 0 ? size : 10);

        Page<Contract> contracts;
        if (roomId != null && status != null) {
            contracts = contractRepository.findByLandlordAndRoomIdAndStatusOrderByCreatedAtDesc(landlord, roomId, status, pageable);
        } else if (roomId != null) {
            contracts = contractRepository.findByLandlordAndRoomIdOrderByCreatedAtDesc(landlord, roomId, pageable);
        } else if (status != null) {
            contracts = contractRepository.findByLandlordAndStatusOrderByCreatedAtDesc(landlord, status, pageable);
        } else {
            contracts = contractRepository.findByLandlordOrderByCreatedAtDesc(landlord, pageable);
        }

        return PageResponse.from(contracts.map(contractMapper::toContractResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContractResponse> getAllContractsForAdmin(ContractStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), size > 0 ? size : 10);

        Page<Contract> contracts = (status != null)
                ? contractRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : contractRepository.findAllByOrderByCreatedAtDesc(pageable);

        return PageResponse.from(contracts.map(contractMapper::toContractResponse));
    }

    @Override
    @Transactional
    public ContractResponse updateContractStatus(UUID contractId, UpdateContractStatusRequest request, String userEmail) {
        Contract contract = getContractEntityById(contractId);
        User user = getUserByEmail(userEmail);

        boolean isTenant = contract.getTenant().getId().equals(user.getId());
        boolean isLandlord = contract.getLandlord().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        ContractStatus currentStatus = contract.getStatus();
        ContractStatus targetStatus = request.getStatus();

        if ((targetStatus == ContractStatus.REJECTED || targetStatus == ContractStatus.CANCELLED
                || targetStatus == ContractStatus.TERMINATED)
                && (request.getReason() == null || request.getReason().isBlank())) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Vui lòng nhập lý do xử lý hợp đồng");
        }

        switch (targetStatus) {
            case AWAITING_DEPOSIT:
                // Chỉ Chủ nhà hoặc Admin mới có quyền duyệt hợp đồng
                if (!isLandlord && !isAdmin) {
                    throw new AppException(ErrorCode.UNAUTHORIZED, "Chỉ chủ nhà hoặc quản trị viên mới có quyền phê duyệt hợp đồng");
                }
                if (currentStatus != ContractStatus.PENDING) {
                    throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Chỉ có thể phê duyệt hợp đồng đang ở trạng thái chờ duyệt (PENDING)");
                }

                if (contract.getStartDate().isBefore(today())) {
                    throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Ngày bắt đầu thuê đã qua. Vui lòng gửi yêu cầu mới với thời hạn phù hợp");
                }

                // 1. KHÓA BI QUAN (PESSIMISTIC WRITE LOCK) bản ghi Room:
                // Ngăn chặn race condition khi có 2 hoặc nhiều người/luồng cùng duyệt/ký hợp đồng cho phòng này cùng lúc
                Room lockedRoom = roomRepository.findByIdForUpdate(contract.getRoom().getId())
                        .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy phòng trọ"));

                if (lockedRoom.getStatus() == RoomStatus.RENTED) {
                    throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Phòng trọ này vừa được cho người khác thuê. Không thể phê duyệt thêm hợp đồng!");
                }
                if (lockedRoom.getStatus() != RoomStatus.APPROVED) {
                    throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Phòng trọ hiện chưa sẵn sàng để cho thuê");
                }

                // Kiểm tra xem đã có hợp đồng ACTIVE nào khác cho phòng này chưa
                boolean hasOtherActiveContract = contractRepository.existsByRoomAndStatus(lockedRoom, ContractStatus.ACTIVE);
                if (hasOtherActiveContract) {
                    throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Phòng trọ này đã có một hợp đồng khác đang có hiệu lực");
                }

                // 2. Chuyển trạng thái phòng sang RENTED
                lockedRoom.setStatus(RoomStatus.RESERVED);
                contract.setDepositDeadline(Instant.now().plus(24, ChronoUnit.HOURS));
                roomRepository.save(lockedRoom);

                // 3. Tự động TỪ CHỐI (Auto-reject) tất cả các yêu cầu PENDING khác của phòng này
                List<Contract> competingPendingContracts = contractRepository.findByRoomAndStatusAndIdNot(
                        lockedRoom, ContractStatus.PENDING, contract.getId()
                );
                if (!competingPendingContracts.isEmpty()) {
                    for (Contract competing : competingPendingContracts) {
                        competing.setStatus(ContractStatus.REJECTED);
                        competing.setCancellationReason("Phòng trọ đã được phê duyệt cho một khách hàng khác ký hợp đồng.");
                    }
                    contractRepository.saveAll(competingPendingContracts);
                    log.info("Auto-rejected [{}] competing PENDING contracts for room [{}]", competingPendingContracts.size(), lockedRoom.getId());
                }

                log.info("Request [{}] accepted. Room [{}] reserved for deposit", contract.getContractCode(), lockedRoom.getId());
                break;

            case REJECTED:
                // Chỉ Chủ nhà hoặc Admin có quyền từ chối
                if (!isLandlord && !isAdmin) {
                    throw new AppException(ErrorCode.UNAUTHORIZED, "Chỉ chủ nhà hoặc quản trị viên mới có quyền từ chối yêu cầu thuê");
                }
                if (currentStatus != ContractStatus.PENDING) {
                    throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Chỉ có thể từ chối yêu cầu đang ở trạng thái chờ duyệt (PENDING)");
                }
                contract.setCancellationReason(request.getReason());
                log.info("Contract [{}] was REJECTED by Landlord/Admin [{}]. Reason: {}", contract.getContractCode(), userEmail, request.getReason());
                break;

            case CANCELLED:
                // Người thuê hoặc Admin có quyền hủy yêu cầu khi còn PENDING
                if (!isTenant && !isAdmin) {
                    throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền hủy yêu cầu thuê phòng này");
                }
                if (contract.getDepositPaidAt() != null || (currentStatus != ContractStatus.PENDING && currentStatus != ContractStatus.AWAITING_DEPOSIT)) {
                    throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Chỉ được hủy yêu cầu khi chưa thanh toán cọc");
                }
                if (currentStatus == ContractStatus.AWAITING_DEPOSIT) releaseRoom(contract);
                contract.setCancellationReason(request.getReason());
                log.info("Contract [{}] was CANCELLED by Tenant [{}]. Reason: {}", contract.getContractCode(), userEmail, request.getReason());
                break;

            case TERMINATED:
                throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "Không thể chấm dứt trực tiếp. Hãy gửi yêu cầu chấm dứt trước hạn để bên còn lại xác nhận.");

            case EXPIRED:
                // Admin hoặc hệ thống tự động hết hạn
                if (!isAdmin) {
                    throw new AppException(ErrorCode.UNAUTHORIZED, "Chỉ quản trị viên mới có quyền chuyển trạng thái hết hạn");
                }
                if (currentStatus != ContractStatus.ACTIVE || !contract.getEndDate().isBefore(today())) {
                    throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Chỉ được hết hạn hợp đồng đang hiệu lực và đã qua ngày kết thúc");
                }
                if (contract.getAgreedEndDate() != null) {
                    throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Hợp đồng có lịch chấm dứt đã thống nhất; hệ thống sẽ xử lý theo lịch.");
                }
                lapsePendingClosures(contract);
                releaseRoom(contract);
                break;

            default:
                throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Trạng thái chuyển đổi không hợp lệ");
        }

        contract.setStatus(targetStatus);
        Contract updated = contractRepository.save(contract);

        return contractMapper.toContractResponse(updated);
    }

    @Override
    @Transactional
    public ContractResponse simulateDeposit(UUID contractId, String email) {
        Contract contract = getContractEntityById(contractId);
        User user = getUserByEmail(email);
        if (!contract.getTenant().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Chỉ người thuê của yêu cầu này được thanh toán cọc giả lập");
        }
        if (contract.getDepositPaidAt() != null) return contractMapper.toContractResponse(contract);
        if (contract.getStatus() != ContractStatus.AWAITING_DEPOSIT || contract.getDepositDeadline() == null
                || !contract.getDepositDeadline().isAfter(Instant.now())) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Yêu cầu chưa được mở đặt cọc hoặc đã quá hạn thanh toán");
        }
        Room room = roomRepository.findByIdForUpdate(contract.getRoom().getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        if (room.getStatus() != RoomStatus.RESERVED || contract.getEndDate().isBefore(today())) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Phòng hoặc thời hạn thuê không còn phù hợp để thanh toán");
        }
        Instant now = Instant.now();
        contract.setDepositPaidAt(now);
        contract.setPaymentReference("SIM-PAY-" + UUID.randomUUID());
        contract.setFormalizedAt(now);
        contract.setContractCode("HD-" + today().format(DateTimeFormatter.ofPattern("yyyyMM"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        contract.setDocumentContent("HỢP ĐỒNG THUÊ PHÒNG — BẢN GIẢ LẬP\n"
                + "Mã hợp đồng: " + contract.getContractCode() + "\n"
                + "Chủ phòng: " + contract.getLandlord().getFullName() + " (" + contract.getLandlord().getEmail() + ")\n"
                + "Người thuê: " + contract.getTenant().getFullName() + " (" + contract.getTenant().getEmail() + ")\n"
                + "Phòng: " + room.getTitle() + "\nĐịa chỉ: " + room.getAddress() + "\n"
                + "Thời hạn: " + contract.getStartDate() + " đến " + contract.getEndDate() + "\n"
                + "Tiền thuê mỗi tháng: " + contract.getMonthlyRent() + " VNĐ\n"
                + "Tiền cọc đã xác nhận (giả lập): " + contract.getDepositAmount() + " VNĐ\n"
                + "Điều khoản bổ sung: " + (contract.getTerms() == null ? "Không có" : contract.getTerms()) + "\n"
                + "Hợp đồng có hiệu lực khi cả người thuê và chủ phòng hoàn tất ký giả lập.\n"
                + "Đây là môi trường mô phỏng: không thu tiền thật và không tạo chữ ký số được chứng thực.");
        contract.setStatus(ContractStatus.AWAITING_SIGNATURES);
        return contractMapper.toContractResponse(contractRepository.save(contract));
    }

    @Override
    @Transactional
    public ContractResponse simulateSignature(UUID contractId, String email) {
        Contract contract = getContractEntityById(contractId);
        User user = getUserByEmail(email);
        boolean tenant = contract.getTenant().getId().equals(user.getId());
        boolean landlord = contract.getLandlord().getId().equals(user.getId());
        if (!tenant && !landlord) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Chỉ hai bên trong hợp đồng được ký; admin không được ký thay");
        }
        if (hasOpenClosure(contract)) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Đang có yêu cầu hủy chờ xác nhận, chưa thể ký hợp đồng.");
        }
        if ((contract.getStatus() == ContractStatus.AWAITING_SIGNATURES || contract.getStatus() == ContractStatus.ACTIVE)
                && (tenant ? contract.getTenantSignedAt() : contract.getLandlordSignedAt()) != null) {
            return contractMapper.toContractResponse(contract);
        }
        if (contract.getStatus() != ContractStatus.AWAITING_SIGNATURES || contract.getDepositPaidAt() == null
                || contract.getDocumentContent() == null || contract.getEndDate().isBefore(today())) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Chỉ được ký hợp đồng đã thanh toán cọc, đã tạo nội dung và còn thời hạn");
        }
        Room room = roomRepository.findByIdForUpdate(contract.getRoom().getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        if (room.getStatus() != RoomStatus.RESERVED) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Phòng không còn ở trạng thái giữ chỗ cho hợp đồng");
        }
        Instant now = Instant.now();
        if (tenant) contract.setTenantSignedAt(now);
        else contract.setLandlordSignedAt(now);
        if (contract.getTenantSignedAt() != null && contract.getLandlordSignedAt() != null) {
            contract.setStatus(ContractStatus.ACTIVE);
            contract.setActivatedAt(now);
            room.setStatus(RoomStatus.RENTED);
            roomRepository.save(room);
        }
        return contractMapper.toContractResponse(contractRepository.save(contract));
    }

    private static LocalDate today() {
        return LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    private boolean hasOpenClosure(Contract contract) {
        return contract.getClosureRequests().stream().anyMatch(r ->
                r.getStatus() == ContractClosure.Status.PENDING || r.getStatus() == ContractClosure.Status.ACCEPTED);
    }

    private void requireParty(Contract contract, User user) {
        if (!contract.getTenant().getId().equals(user.getId()) && !contract.getLandlord().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Chỉ hai bên trong hợp đồng được gửi và phản hồi yêu cầu; không được xác nhận thay.");
        }
    }

    private void validateEndDate(Contract contract, LocalDate endDate) {
        if (endDate == null || endDate.isBefore(today()) || endDate.isBefore(contract.getStartDate())
                || !endDate.isBefore(contract.getEndDate())) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Ngày chấm dứt phải từ hôm nay, không trước ngày bắt đầu và trước ngày hết hạn hợp đồng.");
        }
    }

    private void validateRefund(Contract contract, CreateClosureRequest request) {
        Double amount = request.getRefundAmount();
        if (request.getRefundType() == null || amount == null || !Double.isFinite(amount) || amount < 0
                || amount > contract.getDepositAmount() || request.getSettlementNote() == null || request.getSettlementNote().isBlank()) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Vui lòng ghi rõ phương án cọc, số tiền hoàn hợp lệ và căn cứ thỏa thuận.");
        }
        boolean valid = switch (request.getRefundType()) {
            case FULL -> Double.compare(amount, contract.getDepositAmount()) == 0;
            case NONE -> amount == 0;
            case PARTIAL -> amount > 0 && amount < contract.getDepositAmount();
        };
        if (!valid) throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Số tiền hoàn không khớp phương án xử lý cọc.");
    }

    @Override
    @Transactional
    public ContractResponse requestClosure(UUID id, CreateClosureRequest request, String email) {
        Contract contract = getContractEntityById(id);
        User user = getUserByEmail(email);
        requireParty(contract, user);
        boolean active = contract.getStatus() == ContractStatus.ACTIVE;
        if (!active && (contract.getStatus() != ContractStatus.AWAITING_SIGNATURES || contract.getDepositPaidAt() == null)) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Chỉ tạo yêu cầu khi đã cọc chờ ký hoặc hợp đồng đang hiệu lực.");
        }
        if (hasOpenClosure(contract)) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Đã có yêu cầu đang chờ phản hồi hoặc lịch chấm dứt được chấp nhận.");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Vui lòng nhập lý do.");
        }
        if (active) validateEndDate(contract, request.getRequestedEndDate());
        else if (request.getRequestedEndDate() != null) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Yêu cầu hủy trước hiệu lực không có ngày chấm dứt.");
        }
        validateRefund(contract, request);
        contract.getClosureRequests().add(ContractClosure.builder()
                .requestId(UUID.randomUUID())
                .kind(active ? ContractClosure.Kind.EARLY_TERMINATION : ContractClosure.Kind.CANCELLATION)
                .status(ContractClosure.Status.PENDING).requestedBy(user.getId())
                .reason(request.getReason().trim()).requestedEndDate(request.getRequestedEndDate())
                .refundType(request.getRefundType()).refundAmount(request.getRefundAmount())
                .settlementNote(request.getSettlementNote().trim()).requestedAt(Instant.now()).build());
        contract.setUpdatedAt(Instant.now());
        return contractMapper.toContractResponse(contractRepository.save(contract));
    }

    @Override
    @Transactional
    public ContractResponse respondClosure(UUID id, UUID requestId, RespondClosureRequest request, String email) {
        Contract contract = getContractEntityById(id);
        User user = getUserByEmail(email);
        requireParty(contract, user);
        ContractClosure closure = contract.getClosureRequests().stream().filter(r -> r.getRequestId().equals(requestId))
                .findFirst().orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy yêu cầu."));
        if (closure.getRequestedBy().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Người gửi không được tự xác nhận yêu cầu của mình.");
        }
        if (closure.getStatus() != ContractClosure.Status.PENDING || request.getAccepted() == null) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Yêu cầu đã được xử lý hoặc phản hồi không hợp lệ.");
        }
        boolean cancellation = closure.getKind() == ContractClosure.Kind.CANCELLATION;
        if ((cancellation && contract.getStatus() != ContractStatus.AWAITING_SIGNATURES)
                || (!cancellation && contract.getStatus() != ContractStatus.ACTIVE)) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Trạng thái hợp đồng đã thay đổi, không thể xử lý yêu cầu.");
        }
        if (!request.getAccepted() && (request.getReason() == null || request.getReason().isBlank())) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Vui lòng nhập lý do từ chối.");
        }
        if (request.getAccepted() && !cancellation) validateEndDate(contract, closure.getRequestedEndDate());
        closure.setRespondedBy(user.getId());
        closure.setRespondedAt(Instant.now());
        closure.setResponseReason(request.getReason() == null ? null : request.getReason().trim());
        closure.setStatus(request.getAccepted() ? ContractClosure.Status.ACCEPTED : ContractClosure.Status.REJECTED);
        if (request.getAccepted()) {
            if (cancellation) {
                releaseRoom(contract);
                contract.setStatus(ContractStatus.CANCELLED);
                contract.setCancellationReason(closure.getReason());
                closure.setStatus(ContractClosure.Status.COMPLETED);
                closure.setCompletedAt(Instant.now());
            } else {
                contract.setAgreedEndDate(closure.getRequestedEndDate());
                completeScheduledClosure(contract);
            }
        }
        contract.setUpdatedAt(Instant.now());
        return contractMapper.toContractResponse(contractRepository.save(contract));
    }

    private boolean completeScheduledClosure(Contract contract) {
        if (contract.getStatus() != ContractStatus.ACTIVE || contract.getAgreedEndDate() == null
                || contract.getAgreedEndDate().isAfter(today())) return false;
        ContractClosure closure = contract.getClosureRequests().stream()
                .filter(r -> r.getStatus() == ContractClosure.Status.ACCEPTED && r.getKind() == ContractClosure.Kind.EARLY_TERMINATION)
                .findFirst().orElseThrow(() -> new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Thiếu thỏa thuận chấm dứt đã xác nhận."));
        releaseRoom(contract);
        contract.setStatus(ContractStatus.TERMINATED);
        contract.setTerminatedAt(Instant.now());
        contract.setCancellationReason(closure.getReason());
        closure.setStatus(ContractClosure.Status.COMPLETED);
        closure.setCompletedAt(contract.getTerminatedAt());
        return true;
    }

    private void lapsePendingClosures(Contract contract) {
        contract.getClosureRequests().stream().filter(r -> r.getStatus() == ContractClosure.Status.PENDING)
                .forEach(r -> r.setStatus(ContractClosure.Status.LAPSED));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void releaseRoom(Contract contract) {
        Room room = roomRepository.findByIdForUpdate(contract.getRoom().getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy phòng trọ"));
        if ((room.getStatus() == RoomStatus.RENTED || room.getStatus() == RoomStatus.RESERVED)
                && !contractRepository.existsByRoomAndStatusAndIdNot(room, ContractStatus.ACTIVE, contract.getId())) {
            room.setStatus(RoomStatus.APPROVED);
            roomRepository.save(room);
        }
    }

    @Override
    @Transactional
    public void expireContract(UUID contractId) {
        Contract contract = getContractEntityById(contractId);
        if (completeScheduledClosure(contract)) {
            contractRepository.save(contract);
            return;
        }
        boolean unpaidExpired = contract.getStatus() == ContractStatus.AWAITING_DEPOSIT
                && contract.getDepositDeadline() != null && !contract.getDepositDeadline().isAfter(Instant.now());
        if (unpaidExpired || (contract.getStatus() == ContractStatus.ACTIVE && contract.getEndDate().isBefore(today()))) {
            releaseRoom(contract);
            lapsePendingClosures(contract);
            contract.setStatus(ContractStatus.EXPIRED);
            if (unpaidExpired) contract.setCancellationReason("Quá hạn thanh toán cọc 24 giờ");
            contractRepository.save(contract);
        }
    }

    private Contract getContractEntityById(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
    }
}
