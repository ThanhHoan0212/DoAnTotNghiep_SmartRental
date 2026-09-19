package com.phongtro.backend.service.impl;

import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.request.CreateContractRequest;
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

        Room room = roomRepository.findById(request.getRoomId())
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
                tenant, room, List.of(ContractStatus.PENDING, ContractStatus.ACTIVE)
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
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String contractCode = "HD-" + datePrefix + "-" + randomSuffix;

        // 7. Số tiền cọc: nếu không nhập thì mặc định bằng 1 tháng tiền phòng
        Double deposit = (request.getDepositAmount() != null && request.getDepositAmount() > 0)
                ? request.getDepositAmount()
                : room.getPrice();

        Contract contract = Contract.builder()
                .contractCode(contractCode)
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

        switch (targetStatus) {
            case ACTIVE:
                // Chỉ Chủ nhà hoặc Admin mới có quyền duyệt hợp đồng
                if (!isLandlord && !isAdmin) {
                    throw new AppException(ErrorCode.UNAUTHORIZED, "Chỉ chủ nhà hoặc quản trị viên mới có quyền phê duyệt hợp đồng");
                }
                if (currentStatus != ContractStatus.PENDING) {
                    throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Chỉ có thể phê duyệt hợp đồng đang ở trạng thái chờ duyệt (PENDING)");
                }

                // 1. KHÓA BI QUAN (PESSIMISTIC WRITE LOCK) bản ghi Room:
                // Ngăn chặn race condition khi có 2 hoặc nhiều người/luồng cùng duyệt/ký hợp đồng cho phòng này cùng lúc
                Room lockedRoom = roomRepository.findByIdForUpdate(contract.getRoom().getId())
                        .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy phòng trọ"));

                if (lockedRoom.getStatus() == RoomStatus.RENTED) {
                    throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Phòng trọ này vừa được cho người khác thuê. Không thể phê duyệt thêm hợp đồng!");
                }

                // Kiểm tra xem đã có hợp đồng ACTIVE nào khác cho phòng này chưa
                boolean hasOtherActiveContract = contractRepository.existsByRoomAndStatus(lockedRoom, ContractStatus.ACTIVE);
                if (hasOtherActiveContract) {
                    throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Phòng trọ này đã có một hợp đồng khác đang có hiệu lực");
                }

                // 2. Chuyển trạng thái phòng sang RENTED
                lockedRoom.setStatus(RoomStatus.RENTED);
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

                log.info("Contract [{}] is now ACTIVE. Room [{}] status updated to RENTED", contract.getContractCode(), lockedRoom.getId());
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
                if (currentStatus != ContractStatus.PENDING) {
                    throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Chỉ có thể hủy yêu cầu thuê khi chưa được chủ nhà phê duyệt");
                }
                contract.setCancellationReason(request.getReason());
                log.info("Contract [{}] was CANCELLED by Tenant [{}]. Reason: {}", contract.getContractCode(), userEmail, request.getReason());
                break;

            case TERMINATED:
                // Người thuê, Chủ nhà hoặc Admin có thể thanh lý hợp đồng khi đang ACTIVE
                if (!isTenant && !isLandlord && !isAdmin) {
                    throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền thanh lý hợp đồng này");
                }
                if (currentStatus != ContractStatus.ACTIVE) {
                    throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Chỉ có thể thanh lý hợp đồng đang có hiệu lực (ACTIVE)");
                }
                contract.setCancellationReason(request.getReason());

                // Mở lại phòng trọ sang APPROVED nếu không còn hợp đồng ACTIVE nào khác
                Room rentedRoom = contract.getRoom();
                boolean otherActive = contractRepository.existsByRoomAndStatus(rentedRoom, ContractStatus.ACTIVE);
                if (!otherActive) {
                    rentedRoom.setStatus(RoomStatus.APPROVED);
                    roomRepository.save(rentedRoom);
                    log.info("Room [{}] restored to APPROVED after contract termination", rentedRoom.getId());
                }
                break;

            case EXPIRED:
                // Admin hoặc hệ thống tự động hết hạn
                if (!isAdmin) {
                    throw new AppException(ErrorCode.UNAUTHORIZED, "Chỉ quản trị viên mới có quyền chuyển trạng thái hết hạn");
                }
                break;

            default:
                throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Trạng thái chuyển đổi không hợp lệ");
        }

        contract.setStatus(targetStatus);
        Contract updated = contractRepository.save(contract);

        return contractMapper.toContractResponse(updated);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Contract getContractEntityById(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
    }
}
