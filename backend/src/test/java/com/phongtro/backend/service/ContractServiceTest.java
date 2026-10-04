package com.phongtro.backend.service;

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
import com.phongtro.backend.service.impl.ContractServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContractMapper contractMapper;

    @InjectMocks
    private ContractServiceImpl contractService;

    private User sampleTenant;
    private User sampleLandlord;
    private User sampleOtherUser;
    private Room sampleRoom;
    private Contract sampleContract;
    private ContractResponse sampleContractResponse;
    private UUID roomId;
    private UUID contractId;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        contractId = UUID.randomUUID();

        sampleTenant = User.builder()
                .id(UUID.randomUUID())
                .email("tenant@phongtro.vn")
                .fullName("Nguyễn Văn Thuê")
                .role(Role.TENANT)
                .status(UserStatus.ACTIVE)
                .isIdentityVerified(true)
                .build();

        sampleLandlord = User.builder()
                .id(UUID.randomUUID())
                .email("landlord@phongtro.vn")
                .fullName("Trần Thị Chủ Nhà")
                .role(Role.LANDLORD)
                .status(UserStatus.ACTIVE)
                .isIdentityVerified(true)
                .build();

        sampleOtherUser = User.builder()
                .id(UUID.randomUUID())
                .email("other@phongtro.vn")
                .fullName("Người Lạ Không Liên Quan")
                .role(Role.TENANT)
                .status(UserStatus.ACTIVE)
                .isIdentityVerified(true)
                .build();

        sampleRoom = Room.builder()
                .id(roomId)
                .landlord(sampleLandlord)
                .title("Phòng trọ cao cấp Bình Thạnh")
                .price(4000000.0)
                .area(25.0)
                .address("123 Điện Biên Phủ")
                .district("Bình Thạnh")
                .city("Hồ Chí Minh")
                .status(RoomStatus.APPROVED)
                .build();

        sampleContract = Contract.builder()
                .id(contractId)
                .contractCode("HD-202609-ABC123")
                .tenant(sampleTenant)
                .landlord(sampleLandlord)
                .room(sampleRoom)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusMonths(6))
                .monthlyRent(4000000.0)
                .depositAmount(4000000.0)
                .status(ContractStatus.PENDING)
                .terms("Gửi cọc giữ phòng trước")
                .build();

        sampleContractResponse = ContractResponse.builder()
                .id(contractId)
                .contractCode("HD-202609-ABC123")
                .roomId(roomId)
                .roomTitle(sampleRoom.getTitle())
                .tenantId(sampleTenant.getId())
                .tenantName(sampleTenant.getFullName())
                .landlordId(sampleLandlord.getId())
                .landlordName(sampleLandlord.getFullName())
                .monthlyRent(4000000.0)
                .depositAmount(4000000.0)
                .status(ContractStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Người thuê gửi yêu cầu thuê phòng: Dữ liệu hợp lệ -> Tạo thành công hợp đồng PENDING")
    void createContractRequest_Success_ShouldCreatePendingContract() {
        CreateContractRequest request = CreateContractRequest.builder()
                .roomId(roomId)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusMonths(6))
                .depositAmount(4000000.0)
                .terms("Em muốn chuyển vào đầu tháng sau")
                .build();

        when(userRepository.findByEmail(sampleTenant.getEmail())).thenReturn(Optional.of(sampleTenant));
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));
        when(contractRepository.existsByTenantAndRoomAndStatusIn(eq(sampleTenant), eq(sampleRoom), any())).thenReturn(false);
        when(contractRepository.existsByRoomAndStatus(sampleRoom, ContractStatus.ACTIVE)).thenReturn(false);
        when(contractRepository.save(any(Contract.class))).thenReturn(sampleContract);
        when(contractMapper.toContractResponse(sampleContract)).thenReturn(sampleContractResponse);

        ContractResponse response = contractService.createContractRequest(request, sampleTenant.getEmail());

        assertNotNull(response);
        assertEquals(contractId, response.getId());
        assertEquals(ContractStatus.PENDING, response.getStatus());
        verify(contractRepository, times(1)).save(any(Contract.class));
    }

    @Test
    @DisplayName("Gửi yêu cầu thuê phòng: Bắt lỗi EKYC_REQUIRED khi người thuê chưa xác thực eKYC")
    void createContractRequest_WhenTenantNotVerified_ShouldThrowEkycRequiredException() {
        sampleTenant.setIdentityVerified(false);
        CreateContractRequest request = CreateContractRequest.builder()
                .roomId(roomId)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusMonths(6))
                .build();

        when(userRepository.findByEmail(sampleTenant.getEmail())).thenReturn(Optional.of(sampleTenant));

        AppException ex = assertThrows(AppException.class, () ->
                contractService.createContractRequest(request, sampleTenant.getEmail()));

        assertEquals(ErrorCode.EKYC_REQUIRED, ex.getErrorCode());
        verify(roomRepository, never()).findByIdForUpdate(any());
        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("Gửi yêu cầu thuê phòng: Bắt lỗi khi phòng chưa được duyệt hoặc đã có người thuê")
    void createContractRequest_WhenRoomNotApproved_ShouldThrowException() {
        sampleRoom.setStatus(RoomStatus.RENTED);

        CreateContractRequest request = CreateContractRequest.builder()
                .roomId(roomId)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusMonths(6))
                .build();

        when(userRepository.findByEmail(sampleTenant.getEmail())).thenReturn(Optional.of(sampleTenant));
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));

        AppException ex = assertThrows(AppException.class, () ->
                contractService.createContractRequest(request, sampleTenant.getEmail()));

        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, ex.getErrorCode());
        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("Gửi yêu cầu thuê phòng: Bắt lỗi khi chủ nhà tự gửi yêu cầu thuê phòng của chính mình")
    void createContractRequest_WhenTenantIsLandlord_ShouldThrowException() {
        CreateContractRequest request = CreateContractRequest.builder()
                .roomId(roomId)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusMonths(6))
                .build();

        when(userRepository.findByEmail(sampleLandlord.getEmail())).thenReturn(Optional.of(sampleLandlord));
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));

        AppException ex = assertThrows(AppException.class, () ->
                contractService.createContractRequest(request, sampleLandlord.getEmail()));

        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, ex.getErrorCode());
        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("Gửi yêu cầu thuê phòng: Bắt lỗi khi ngày kết thúc trước ngày bắt đầu")
    void createContractRequest_WhenEndDateBeforeStartDate_ShouldThrowException() {
        CreateContractRequest request = CreateContractRequest.builder()
                .roomId(roomId)
                .startDate(LocalDate.now().plusMonths(2))
                .endDate(LocalDate.now().plusMonths(1)) // Sai logic ngày
                .build();

        when(userRepository.findByEmail(sampleTenant.getEmail())).thenReturn(Optional.of(sampleTenant));
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));

        AppException ex = assertThrows(AppException.class, () ->
                contractService.createContractRequest(request, sampleTenant.getEmail()));

        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Xem chi tiết hợp đồng: Người thuê hoặc Chủ nhà có quyền xem chi tiết")
    void getContractById_WhenAuthorized_ShouldReturnContractResponse() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(userRepository.findByEmail(sampleTenant.getEmail())).thenReturn(Optional.of(sampleTenant));
        when(contractMapper.toContractResponse(sampleContract)).thenReturn(sampleContractResponse);

        ContractResponse response = contractService.getContractById(contractId, sampleTenant.getEmail());

        assertNotNull(response);
        assertEquals(contractId, response.getId());
    }

    @Test
    @DisplayName("Xem chi tiết hợp đồng: Người ngoài không liên quan bị chặn quyền UNAUTHORIZED")
    void getContractById_WhenUnauthorizedUser_ShouldThrowUnauthorizedException() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(userRepository.findByEmail(sampleOtherUser.getEmail())).thenReturn(Optional.of(sampleOtherUser));

        AppException ex = assertThrows(AppException.class, () ->
                contractService.getContractById(contractId, sampleOtherUser.getEmail()));

        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Chủ nhà phê duyệt hợp đồng: Trạng thái chuyển ACTIVE và phòng chuyển RENTED kèm tự động hủy yêu cầu chờ khác")
    void updateContractStatus_WhenLandlordApprovesPending_ShouldSetStatusActiveAndRoomRented() {
        UpdateContractStatusRequest request = UpdateContractStatusRequest.builder()
                .status(ContractStatus.AWAITING_DEPOSIT)
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(userRepository.findByEmail(sampleLandlord.getEmail())).thenReturn(Optional.of(sampleLandlord));
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));
        when(contractRepository.existsByRoomAndStatus(sampleRoom, ContractStatus.ACTIVE)).thenReturn(false);
        when(contractRepository.findByRoomAndStatusAndIdNot(sampleRoom, ContractStatus.PENDING, contractId))
                .thenReturn(List.of());
        when(contractRepository.save(sampleContract)).thenReturn(sampleContract);

        sampleContractResponse.setStatus(ContractStatus.AWAITING_DEPOSIT);
        when(contractMapper.toContractResponse(sampleContract)).thenReturn(sampleContractResponse);

        ContractResponse response = contractService.updateContractStatus(contractId, request, sampleLandlord.getEmail());

        assertNotNull(response);
        assertEquals(ContractStatus.AWAITING_DEPOSIT, sampleContract.getStatus());
        assertEquals(RoomStatus.RESERVED, sampleRoom.getStatus());
        verify(roomRepository, times(1)).findByIdForUpdate(roomId);
        verify(roomRepository, times(1)).save(sampleRoom);
        verify(contractRepository, times(1)).save(sampleContract);
    }

    @Test
    @DisplayName("Xử lý bất đồng bộ (Race Condition): Chặn duyệt hợp đồng khi phòng đã có người thuê trước đó")
    void updateContractStatus_WhenRoomAlreadyRentedByConcurrentThread_ShouldThrowException() {
        UpdateContractStatusRequest request = UpdateContractStatusRequest.builder()
                .status(ContractStatus.AWAITING_DEPOSIT)
                .build();

        // Giả lập phòng đã bị một luồng khác duyệt chuyển sang RENTED trước đó
        sampleRoom.setStatus(RoomStatus.RENTED);

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(userRepository.findByEmail(sampleLandlord.getEmail())).thenReturn(Optional.of(sampleLandlord));
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));

        AppException ex = assertThrows(AppException.class, () ->
                contractService.updateContractStatus(contractId, request, sampleLandlord.getEmail()));

        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("vừa được cho người khác thuê"));
        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chủ nhà từ chối yêu cầu thuê: Trạng thái chuyển REJECTED kèm lý do")
    void updateContractStatus_WhenLandlordRejectsPending_ShouldSetStatusRejectedWithReason() {
        UpdateContractStatusRequest request = UpdateContractStatusRequest.builder()
                .status(ContractStatus.REJECTED)
                .reason("Phòng đã nhận khách đặt trực tiếp")
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(userRepository.findByEmail(sampleLandlord.getEmail())).thenReturn(Optional.of(sampleLandlord));
        when(contractRepository.save(sampleContract)).thenReturn(sampleContract);

        sampleContractResponse.setStatus(ContractStatus.REJECTED);
        when(contractMapper.toContractResponse(sampleContract)).thenReturn(sampleContractResponse);

        ContractResponse response = contractService.updateContractStatus(contractId, request, sampleLandlord.getEmail());

        assertNotNull(response);
        assertEquals(ContractStatus.REJECTED, sampleContract.getStatus());
        assertEquals("Phòng đã nhận khách đặt trực tiếp", sampleContract.getCancellationReason());
    }

    @Test
    @DisplayName("Người thuê tự hủy yêu cầu thuê phòng khi còn PENDING -> Trạng thái chuyển CANCELLED")
    void updateContractStatus_WhenTenantCancelsPending_ShouldSetStatusCancelled() {
        UpdateContractStatusRequest request = UpdateContractStatusRequest.builder()
                .status(ContractStatus.CANCELLED)
                .reason("Em đã tìm được phòng khác gần trường hơn")
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(userRepository.findByEmail(sampleTenant.getEmail())).thenReturn(Optional.of(sampleTenant));
        when(contractRepository.save(sampleContract)).thenReturn(sampleContract);

        sampleContractResponse.setStatus(ContractStatus.CANCELLED);
        when(contractMapper.toContractResponse(sampleContract)).thenReturn(sampleContractResponse);

        ContractResponse response = contractService.updateContractStatus(contractId, request, sampleTenant.getEmail());

        assertNotNull(response);
        assertEquals(ContractStatus.CANCELLED, sampleContract.getStatus());
    }

    @Test
    void directTerminationIsForbiddenEvenForLandlord() {
        sampleContract.setStatus(ContractStatus.ACTIVE);
        sampleRoom.setStatus(RoomStatus.RENTED);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(userRepository.findByEmail(sampleLandlord.getEmail())).thenReturn(Optional.of(sampleLandlord));
        assertThrows(AppException.class, () -> contractService.updateContractStatus(contractId,
                UpdateContractStatusRequest.builder().status(ContractStatus.TERMINATED).reason("Move out").build(), sampleLandlord.getEmail()));
        assertEquals(ContractStatus.ACTIVE, sampleContract.getStatus());
        verify(roomRepository, never()).save(any());
    }

    @Test
    void approveHiddenRoomIsRejected() {
        sampleRoom.setStatus(RoomStatus.HIDDEN);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(userRepository.findByEmail(sampleLandlord.getEmail())).thenReturn(Optional.of(sampleLandlord));
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));
        assertThrows(AppException.class, () -> contractService.updateContractStatus(contractId,
                UpdateContractStatusRequest.builder().status(ContractStatus.AWAITING_DEPOSIT).build(), sampleLandlord.getEmail()));
        verify(contractRepository, never()).save(any());
    }

    @Test
    void approvePastStartDateIsRejected() {
        sampleContract.setStartDate(LocalDate.now().minusDays(1));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(userRepository.findByEmail(sampleLandlord.getEmail())).thenReturn(Optional.of(sampleLandlord));
        assertThrows(AppException.class, () -> contractService.updateContractStatus(contractId,
                UpdateContractStatusRequest.builder().status(ContractStatus.AWAITING_DEPOSIT).build(), sampleLandlord.getEmail()));
        verify(roomRepository, never()).save(any());
    }

    @Test
    void zeroDepositIsPreserved() {
        when(userRepository.findByEmail(sampleTenant.getEmail())).thenReturn(Optional.of(sampleTenant));
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));
        contractService.createContractRequest(CreateContractRequest.builder().roomId(roomId)
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusMonths(1)).depositAmount(0.0).build(), sampleTenant.getEmail());
        verify(contractRepository).save(argThat(c -> c.getDepositAmount() == 0.0));
    }

    @Test
    void negativeDepositIsRejected() {
        when(userRepository.findByEmail(sampleTenant.getEmail())).thenReturn(Optional.of(sampleTenant));
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));
        assertThrows(AppException.class, () -> contractService.createContractRequest(CreateContractRequest.builder()
                .roomId(roomId).startDate(LocalDate.now()).endDate(LocalDate.now().plusMonths(1))
                .depositAmount(-1.0).build(), sampleTenant.getEmail()));
        verify(contractRepository, never()).save(any());
    }

    @Test
    void expiryReleasesRentedRoom() {
        sampleContract.setStatus(ContractStatus.ACTIVE);
        sampleContract.setEndDate(LocalDate.now().minusDays(1));
        sampleRoom.setStatus(RoomStatus.RENTED);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));
        contractService.expireContract(contractId);
        assertEquals(ContractStatus.EXPIRED, sampleContract.getStatus());
        assertEquals(RoomStatus.APPROVED, sampleRoom.getStatus());
        verify(contractRepository).existsByRoomAndStatusAndIdNot(sampleRoom, ContractStatus.ACTIVE, contractId);
    }

    @Test
    void expiryDoesNotEndAgreementOnItsLastDay() {
        sampleContract.setStatus(ContractStatus.ACTIVE);
        sampleContract.setEndDate(LocalDate.now());
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        contractService.expireContract(contractId);
        assertEquals(ContractStatus.ACTIVE, sampleContract.getStatus());
        verify(roomRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void adminCannotExpirePendingOrFutureAgreement() {
        sampleOtherUser.setRole(Role.ADMIN);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(userRepository.findByEmail(sampleOtherUser.getEmail())).thenReturn(Optional.of(sampleOtherUser));
        var request = UpdateContractStatusRequest.builder().status(ContractStatus.EXPIRED).build();
        assertThrows(AppException.class, () -> contractService.updateContractStatus(contractId, request, sampleOtherUser.getEmail()));
        sampleContract.setStatus(ContractStatus.ACTIVE);
        assertThrows(AppException.class, () -> contractService.updateContractStatus(contractId, request, sampleOtherUser.getEmail()));
        verify(contractRepository, never()).save(any());
    }

    @Test
    void scheduledTerminationDoesNotReopenHiddenRoom() {
        prepareAcceptedClosure(LocalDate.now());
        sampleRoom.setStatus(RoomStatus.HIDDEN);
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));
        contractService.expireContract(contractId);
        assertEquals(ContractStatus.TERMINATED, sampleContract.getStatus());
        assertEquals(RoomStatus.HIDDEN, sampleRoom.getStatus());
        verify(roomRepository, never()).save(any());
    }

    private void prepareSimulation(ContractStatus status, User actor) {
        sampleContract.setStatus(status);
        sampleRoom.setStatus(RoomStatus.RESERVED);
        sampleContract.setDepositDeadline(java.time.Instant.now().plusSeconds(3600));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(userRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
    }

    @Test
    void depositCreatesFormalContractButDoesNotActivate() {
        prepareSimulation(ContractStatus.AWAITING_DEPOSIT, sampleTenant);
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));
        when(contractRepository.save(sampleContract)).thenReturn(sampleContract);
        contractService.simulateDeposit(contractId, sampleTenant.getEmail());
        assertEquals(ContractStatus.AWAITING_SIGNATURES, sampleContract.getStatus());
        assertEquals(RoomStatus.RESERVED, sampleRoom.getStatus());
        assertNotNull(sampleContract.getDepositPaidAt());
        assertTrue(sampleContract.getPaymentReference().startsWith("SIM-PAY-"));
        assertTrue(sampleContract.getDocumentContent().contains(sampleTenant.getFullName()));
        assertNotNull(sampleContract.getFormalizedAt());
        assertNull(sampleContract.getActivatedAt());
        assertNull(sampleContract.getTenantSignedAt());
    }

    @Test
    void onlyTenantCanPayEvenWhenActorIsAdmin() {
        sampleOtherUser.setRole(Role.ADMIN);
        prepareSimulation(ContractStatus.AWAITING_DEPOSIT, sampleOtherUser);
        assertThrows(AppException.class, () -> contractService.simulateDeposit(contractId, sampleOtherUser.getEmail()));
        verify(contractRepository, never()).save(any());
    }

    @Test
    void paymentBeforeApprovalIsRejected() {
        prepareSimulation(ContractStatus.PENDING, sampleTenant);
        assertThrows(AppException.class, () -> contractService.simulateDeposit(contractId, sampleTenant.getEmail()));
    }

    @Test
    void paymentAfterDeadlineIsRejected() {
        prepareSimulation(ContractStatus.AWAITING_DEPOSIT, sampleTenant);
        sampleContract.setDepositDeadline(java.time.Instant.now().minusSeconds(1));
        assertThrows(AppException.class, () -> contractService.simulateDeposit(contractId, sampleTenant.getEmail()));
    }

    @Test
    void paymentRetryDoesNotCreateAnotherTransaction() {
        prepareSimulation(ContractStatus.AWAITING_SIGNATURES, sampleTenant);
        sampleContract.setDepositPaidAt(java.time.Instant.now());
        sampleContract.setPaymentReference("SIM-PAY-existing");
        contractService.simulateDeposit(contractId, sampleTenant.getEmail());
        assertEquals("SIM-PAY-existing", sampleContract.getPaymentReference());
        verify(contractRepository, never()).save(any());
    }

    @Test
    void firstSignatureKeepsRoomReservedAndSecondActivates() {
        prepareSimulation(ContractStatus.AWAITING_SIGNATURES, sampleTenant);
        sampleContract.setDepositPaidAt(java.time.Instant.now());
        sampleContract.setDocumentContent("Frozen agreement");
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));
        when(contractRepository.save(sampleContract)).thenReturn(sampleContract);
        contractService.simulateSignature(contractId, sampleTenant.getEmail());
        assertNotNull(sampleContract.getTenantSignedAt());
        assertNull(sampleContract.getLandlordSignedAt());
        assertEquals(ContractStatus.AWAITING_SIGNATURES, sampleContract.getStatus());
        assertEquals(RoomStatus.RESERVED, sampleRoom.getStatus());
        when(userRepository.findByEmail(sampleLandlord.getEmail())).thenReturn(Optional.of(sampleLandlord));
        contractService.simulateSignature(contractId, sampleLandlord.getEmail());
        assertEquals(ContractStatus.ACTIVE, sampleContract.getStatus());
        assertEquals(RoomStatus.RENTED, sampleRoom.getStatus());
        assertNotNull(sampleContract.getActivatedAt());
        assertEquals("Frozen agreement", sampleContract.getDocumentContent());
    }

    @Test
    void landlordCanSignFirstAndDuplicateSigningDoesNotActivate() {
        prepareSimulation(ContractStatus.AWAITING_SIGNATURES, sampleLandlord);
        sampleContract.setDepositPaidAt(java.time.Instant.now());
        sampleContract.setDocumentContent("Agreement");
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));
        when(contractRepository.save(sampleContract)).thenReturn(sampleContract);
        contractService.simulateSignature(contractId, sampleLandlord.getEmail());
        var signedAt = sampleContract.getLandlordSignedAt();
        contractService.simulateSignature(contractId, sampleLandlord.getEmail());
        assertEquals(signedAt, sampleContract.getLandlordSignedAt());
        assertEquals(ContractStatus.AWAITING_SIGNATURES, sampleContract.getStatus());
        verify(contractRepository, times(1)).save(any());
    }

    @Test
    void adminCannotSignForEitherParty() {
        sampleOtherUser.setRole(Role.ADMIN);
        prepareSimulation(ContractStatus.AWAITING_SIGNATURES, sampleOtherUser);
        assertThrows(AppException.class, () -> contractService.simulateSignature(contractId, sampleOtherUser.getEmail()));
    }

    @Test
    void signingBeforeDepositIsRejected() {
        prepareSimulation(ContractStatus.AWAITING_DEPOSIT, sampleTenant);
        assertThrows(AppException.class, () -> contractService.simulateSignature(contractId, sampleTenant.getEmail()));
    }

    @Test
    void statusPatchCannotBypassPaymentAndSignatures() {
        prepareSimulation(ContractStatus.PENDING, sampleLandlord);
        assertThrows(AppException.class, () -> contractService.updateContractStatus(contractId,
            UpdateContractStatusRequest.builder().status(ContractStatus.ACTIVE).build(), sampleLandlord.getEmail()));
        verify(contractRepository, never()).save(any());
    }

    @Test
    void depositTimeoutReleasesReservation() {
        sampleContract.setStatus(ContractStatus.AWAITING_DEPOSIT);
        sampleContract.setDepositDeadline(java.time.Instant.now().minusSeconds(1));
        sampleRoom.setStatus(RoomStatus.RESERVED);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));
        contractService.expireContract(contractId);
        assertEquals(ContractStatus.EXPIRED, sampleContract.getStatus());
        assertEquals(RoomStatus.APPROVED, sampleRoom.getStatus());
    }

    private CreateClosureRequest proposal(LocalDate endDate) {
        return CreateClosureRequest.builder().reason("Dọn đi sớm").requestedEndDate(endDate)
                .refundType(ContractClosure.Refund.PARTIAL).refundAmount(2000000.0)
                .settlementNote("Hai bên thỏa thuận giữ lại một nửa tiền cọc").build();
    }

    private ContractClosure pendingClosure(ContractClosure.Kind kind, LocalDate endDate) {
        return ContractClosure.builder().requestId(UUID.randomUUID()).kind(kind).status(ContractClosure.Status.PENDING)
                .requestedBy(sampleTenant.getId()).reason("Dọn đi sớm").requestedEndDate(endDate)
                .refundType(ContractClosure.Refund.PARTIAL).refundAmount(2000000.0)
                .settlementNote("Giữ lại một nửa").requestedAt(java.time.Instant.now()).build();
    }

    private void prepareClosureActor(ContractStatus status, User actor) {
        sampleContract.setStatus(status);
        sampleContract.setStartDate(LocalDate.now().minusMonths(1));
        sampleContract.setDepositPaidAt(java.time.Instant.now());
        sampleRoom.setStatus(status == ContractStatus.ACTIVE ? RoomStatus.RENTED : RoomStatus.RESERVED);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(userRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
    }

    private void prepareAcceptedClosure(LocalDate date) {
        sampleContract.setStatus(ContractStatus.ACTIVE);
        sampleContract.setAgreedEndDate(date);
        sampleRoom.setStatus(RoomStatus.RENTED);
        ContractClosure closure = pendingClosure(ContractClosure.Kind.EARLY_TERMINATION, date);
        closure.setStatus(ContractClosure.Status.ACCEPTED);
        sampleContract.getClosureRequests().add(closure);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
    }

    @Test
    void tenantCanCancelUnpaidReservationAndReleaseRoom() {
        prepareSimulation(ContractStatus.AWAITING_DEPOSIT, sampleTenant);
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));
        contractService.updateContractStatus(contractId, UpdateContractStatusRequest.builder()
                .status(ContractStatus.CANCELLED).reason("Không thuê nữa").build(), sampleTenant.getEmail());
        assertEquals(ContractStatus.CANCELLED, sampleContract.getStatus());
        assertEquals(RoomStatus.APPROVED, sampleRoom.getStatus());
    }

    @Test
    void paidCancellationMustWaitForOtherPartyAndBlocksSigning() {
        prepareClosureActor(ContractStatus.AWAITING_SIGNATURES, sampleTenant);
        contractService.requestClosure(contractId, proposal(null), sampleTenant.getEmail());
        assertEquals(ContractStatus.AWAITING_SIGNATURES, sampleContract.getStatus());
        assertEquals(RoomStatus.RESERVED, sampleRoom.getStatus());
        assertEquals(ContractClosure.Kind.CANCELLATION, sampleContract.getClosureRequests().get(0).getKind());
        assertThrows(AppException.class, () -> contractService.simulateSignature(contractId, sampleTenant.getEmail()));
        assertThrows(AppException.class, () -> contractService.updateContractStatus(contractId,
                UpdateContractStatusRequest.builder().status(ContractStatus.CANCELLED).reason("Cancel").build(), sampleTenant.getEmail()));
        verify(roomRepository, never()).save(any());
    }

    @Test
    void agreedPaidCancellationReleasesRoomAndPreservesSettlement() {
        prepareClosureActor(ContractStatus.AWAITING_SIGNATURES, sampleLandlord);
        ContractClosure closure = pendingClosure(ContractClosure.Kind.CANCELLATION, null);
        sampleContract.getClosureRequests().add(closure);
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));
        contractService.respondClosure(contractId, closure.getRequestId(), RespondClosureRequest.builder().accepted(true).build(), sampleLandlord.getEmail());
        assertEquals(ContractStatus.CANCELLED, sampleContract.getStatus());
        assertEquals(RoomStatus.APPROVED, sampleRoom.getStatus());
        assertEquals(ContractClosure.Status.COMPLETED, closure.getStatus());
        assertEquals(2000000.0, closure.getRefundAmount());
        assertEquals(sampleLandlord.getId(), closure.getRespondedBy());
        assertNotNull(sampleContract.getDepositPaidAt());
    }

    @Test
    void approvedFutureTerminationKeepsContractAndRoomActive() {
        prepareClosureActor(ContractStatus.ACTIVE, sampleLandlord);
        var closure = pendingClosure(ContractClosure.Kind.EARLY_TERMINATION, LocalDate.now().plusDays(7));
        sampleContract.getClosureRequests().add(closure);
        contractService.respondClosure(contractId, closure.getRequestId(), RespondClosureRequest.builder().accepted(true).build(), sampleLandlord.getEmail());
        assertEquals(ContractStatus.ACTIVE, sampleContract.getStatus());
        assertEquals(RoomStatus.RENTED, sampleRoom.getStatus());
        assertEquals(closure.getRequestedEndDate(), sampleContract.getAgreedEndDate());
        assertEquals(ContractClosure.Status.ACCEPTED, closure.getStatus());
        assertNull(closure.getCompletedAt());
        verify(roomRepository, never()).save(any());
    }

    @Test
    void rejectionKeepsAgreementAndAllowsNewProposalWithoutLosingHistory() {
        prepareClosureActor(ContractStatus.ACTIVE, sampleLandlord);
        var closure = pendingClosure(ContractClosure.Kind.EARLY_TERMINATION, LocalDate.now().plusDays(7));
        sampleContract.getClosureRequests().add(closure);
        contractService.respondClosure(contractId, closure.getRequestId(), RespondClosureRequest.builder().accepted(false).reason("Chưa thống nhất cọc").build(), sampleLandlord.getEmail());
        assertEquals(ContractClosure.Status.REJECTED, closure.getStatus());
        assertNull(sampleContract.getAgreedEndDate());
        assertEquals(ContractStatus.ACTIVE, sampleContract.getStatus());
        contractService.requestClosure(contractId, proposal(LocalDate.now().plusDays(8)), sampleLandlord.getEmail());
        assertEquals(2, sampleContract.getClosureRequests().size());
        verify(roomRepository, never()).save(any());
    }

    @Test
    void senderCannotApproveOwnRequest() {
        prepareClosureActor(ContractStatus.ACTIVE, sampleTenant);
        var closure = pendingClosure(ContractClosure.Kind.EARLY_TERMINATION, LocalDate.now().plusDays(7));
        sampleContract.getClosureRequests().add(closure);
        assertThrows(AppException.class, () -> contractService.respondClosure(contractId, closure.getRequestId(),
                RespondClosureRequest.builder().accepted(true).build(), sampleTenant.getEmail()));
        verify(contractRepository, never()).save(any());
    }

    @Test
    void unrelatedAdminCannotRequestOrApproveClosure() {
        sampleOtherUser.setRole(Role.ADMIN);
        prepareClosureActor(ContractStatus.ACTIVE, sampleOtherUser);
        assertThrows(AppException.class, () -> contractService.requestClosure(contractId, proposal(LocalDate.now().plusDays(1)), sampleOtherUser.getEmail()));
        assertThrows(AppException.class, () -> contractService.respondClosure(contractId, UUID.randomUUID(),
                RespondClosureRequest.builder().accepted(true).build(), sampleOtherUser.getEmail()));
    }

    @Test
    void invalidDatesAndRefundsAreRejected() {
        prepareClosureActor(ContractStatus.ACTIVE, sampleTenant);
        for (LocalDate date : new LocalDate[]{null, LocalDate.now().minusDays(1), sampleContract.getEndDate()}) {
            assertThrows(AppException.class, () -> contractService.requestClosure(contractId, proposal(date), sampleTenant.getEmail()));
        }
        for (double amount : new double[]{-1, 0, 4000000, 5000000, Double.NaN, Double.POSITIVE_INFINITY}) {
            var request = proposal(LocalDate.now().plusDays(1));
            request.setRefundAmount(amount);
            assertThrows(AppException.class, () -> contractService.requestClosure(contractId, request, sampleTenant.getEmail()));
        }
        verify(contractRepository, never()).save(any());
    }

    @Test
    void duplicatePendingRequestAndRepeatedResponseAreRejected() {
        prepareClosureActor(ContractStatus.ACTIVE, sampleLandlord);
        var closure = pendingClosure(ContractClosure.Kind.EARLY_TERMINATION, LocalDate.now().plusDays(7));
        sampleContract.getClosureRequests().add(closure);
        assertThrows(AppException.class, () -> contractService.requestClosure(contractId, proposal(LocalDate.now().plusDays(2)), sampleLandlord.getEmail()));
        closure.setStatus(ContractClosure.Status.ACCEPTED);
        assertThrows(AppException.class, () -> contractService.respondClosure(contractId, closure.getRequestId(), RespondClosureRequest.builder().accepted(true).build(), sampleLandlord.getEmail()));
        verify(contractRepository, never()).save(any());
    }

    @Test
    void scheduledTerminationRunsOnAgreedDateAndIsIdempotent() {
        prepareAcceptedClosure(LocalDate.now());
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));
        contractService.expireContract(contractId);
        contractService.expireContract(contractId);
        assertEquals(ContractStatus.TERMINATED, sampleContract.getStatus());
        assertEquals(RoomStatus.APPROVED, sampleRoom.getStatus());
        assertNotNull(sampleContract.getTerminatedAt());
        assertEquals(ContractClosure.Status.COMPLETED, sampleContract.getClosureRequests().get(0).getStatus());
        verify(roomRepository, times(1)).save(any());
    }

    @Test
    void schedulerDoesNotTerminateBeforeAgreedDate() {
        prepareAcceptedClosure(LocalDate.now().plusDays(1));
        contractService.expireContract(contractId);
        assertEquals(ContractStatus.ACTIVE, sampleContract.getStatus());
        assertEquals(RoomStatus.RENTED, sampleRoom.getStatus());
        verify(roomRepository, never()).save(any());
    }

    @Test
    void staleRequestedDateCannotBeAcceptedButCanBeRejected() {
        prepareClosureActor(ContractStatus.ACTIVE, sampleLandlord);
        var closure = pendingClosure(ContractClosure.Kind.EARLY_TERMINATION, LocalDate.now().minusDays(1));
        sampleContract.getClosureRequests().add(closure);
        assertThrows(AppException.class, () -> contractService.respondClosure(contractId, closure.getRequestId(), RespondClosureRequest.builder().accepted(true).build(), sampleLandlord.getEmail()));
        contractService.respondClosure(contractId, closure.getRequestId(), RespondClosureRequest.builder().accepted(false).reason("Ngày đã qua").build(), sampleLandlord.getEmail());
        assertEquals(ContractClosure.Status.REJECTED, closure.getStatus());
    }

    @Test
    void naturalExpiryLapsesPendingTerminationRequest() {
        sampleContract.setStatus(ContractStatus.ACTIVE);
        sampleContract.setEndDate(LocalDate.now().minusDays(1));
        sampleRoom.setStatus(RoomStatus.RENTED);
        var closure = pendingClosure(ContractClosure.Kind.EARLY_TERMINATION, LocalDate.now().minusDays(2));
        sampleContract.getClosureRequests().add(closure);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));
        contractService.expireContract(contractId);
        assertEquals(ContractStatus.EXPIRED, sampleContract.getStatus());
        assertEquals(ContractClosure.Status.LAPSED, closure.getStatus());
    }
}
