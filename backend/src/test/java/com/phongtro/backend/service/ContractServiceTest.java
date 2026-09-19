package com.phongtro.backend.service;

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
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
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
        verify(roomRepository, never()).findById(any());
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
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));

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
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));

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
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));

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
                .status(ContractStatus.ACTIVE)
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(userRepository.findByEmail(sampleLandlord.getEmail())).thenReturn(Optional.of(sampleLandlord));
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(sampleRoom));
        when(contractRepository.existsByRoomAndStatus(sampleRoom, ContractStatus.ACTIVE)).thenReturn(false);
        when(contractRepository.findByRoomAndStatusAndIdNot(sampleRoom, ContractStatus.PENDING, contractId))
                .thenReturn(List.of());
        when(contractRepository.save(sampleContract)).thenReturn(sampleContract);

        sampleContractResponse.setStatus(ContractStatus.ACTIVE);
        when(contractMapper.toContractResponse(sampleContract)).thenReturn(sampleContractResponse);

        ContractResponse response = contractService.updateContractStatus(contractId, request, sampleLandlord.getEmail());

        assertNotNull(response);
        assertEquals(ContractStatus.ACTIVE, sampleContract.getStatus());
        assertEquals(RoomStatus.RENTED, sampleRoom.getStatus());
        verify(roomRepository, times(1)).findByIdForUpdate(roomId);
        verify(roomRepository, times(1)).save(sampleRoom);
        verify(contractRepository, times(1)).save(sampleContract);
    }

    @Test
    @DisplayName("Xử lý bất đồng bộ (Race Condition): Chặn duyệt hợp đồng khi phòng đã có người thuê trước đó")
    void updateContractStatus_WhenRoomAlreadyRentedByConcurrentThread_ShouldThrowException() {
        UpdateContractStatusRequest request = UpdateContractStatusRequest.builder()
                .status(ContractStatus.ACTIVE)
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
    @DisplayName("Thanh lý hợp đồng ACTIVE: Trạng thái chuyển TERMINATED và phòng hoàn lại APPROVED")
    void updateContractStatus_WhenTerminated_ShouldRevertRoomToApproved() {
        sampleContract.setStatus(ContractStatus.ACTIVE);
        sampleRoom.setStatus(RoomStatus.RENTED);

        UpdateContractStatusRequest request = UpdateContractStatusRequest.builder()
                .status(ContractStatus.TERMINATED)
                .reason("Hết hạn hợp đồng và trả phòng")
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(userRepository.findByEmail(sampleLandlord.getEmail())).thenReturn(Optional.of(sampleLandlord));
        when(contractRepository.existsByRoomAndStatus(sampleRoom, ContractStatus.ACTIVE)).thenReturn(false);
        when(contractRepository.save(sampleContract)).thenReturn(sampleContract);

        sampleContractResponse.setStatus(ContractStatus.TERMINATED);
        when(contractMapper.toContractResponse(sampleContract)).thenReturn(sampleContractResponse);

        ContractResponse response = contractService.updateContractStatus(contractId, request, sampleLandlord.getEmail());

        assertNotNull(response);
        assertEquals(ContractStatus.TERMINATED, sampleContract.getStatus());
        assertEquals(RoomStatus.APPROVED, sampleRoom.getStatus());
        verify(roomRepository, times(1)).save(sampleRoom);
    }
}
