package com.phongtro.backend.service;

import com.phongtro.backend.dto.request.CreateRoomRequest;
import com.phongtro.backend.dto.request.UpdateRoomRequest;
import com.phongtro.backend.dto.response.RoomResponse;
import com.phongtro.backend.entity.*;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.mapper.RoomMapper;
import com.phongtro.backend.repository.AmenityRepository;
import com.phongtro.backend.repository.RoomRepository;
import com.phongtro.backend.repository.UserRepository;
import com.phongtro.backend.service.impl.RoomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AmenityRepository amenityRepository;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomServiceImpl roomService;

    private User sampleLandlord;
    private User sampleTenant;
    private User sampleAdmin;
    private Room sampleRoom;
    private RoomResponse sampleRoomResponse;

    @BeforeEach
    void setUp() {
        UUID landlordId = UUID.randomUUID();
        sampleLandlord = User.builder()
                .id(landlordId)
                .email("landlord@phongtro.vn")
                .fullName("Chủ Nhà Nguyễn Văn B")
                .role(Role.LANDLORD)
                .status(UserStatus.ACTIVE)
                .build();

        sampleTenant = User.builder()
                .id(UUID.randomUUID())
                .email("tenant@phongtro.vn")
                .fullName("Người Thuê Nguyễn Văn C")
                .role(Role.TENANT)
                .status(UserStatus.ACTIVE)
                .build();

        sampleAdmin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@phongtro.vn")
                .fullName("Quản Trị Viên")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        UUID roomId = UUID.randomUUID();
        sampleRoom = Room.builder()
                .id(roomId)
                .landlord(sampleLandlord)
                .title("Phòng trọ Cầu Giấy")
                .description("Mô tả phòng trọ")
                .price(3500000.0)
                .area(25.0)
                .address("Số 10 Cầu Giấy")
                .district("Cầu Giấy")
                .city("Hà Nội")
                .status(RoomStatus.PENDING)
                .viewCount(10L)
                .images(new ArrayList<>())
                .amenities(new HashSet<>())
                .build();

        sampleRoomResponse = RoomResponse.builder()
                .id(roomId)
                .title("Phòng trọ Cầu Giấy")
                .price(3500000.0)
                .area(25.0)
                .district("Cầu Giấy")
                .status(RoomStatus.PENDING)
                .viewCount(10L)
                .build();
    }

    @Test
    @DisplayName("Chủ nhà đăng tin thành công - trạng thái ban đầu là PENDING")
    void createRoom_ByLandlord_Success_PendingStatus() {
        CreateRoomRequest request = CreateRoomRequest.builder()
                .title("Phòng trọ Cầu Giấy")
                .price(3500000.0)
                .area(25.0)
                .address("Số 10 Cầu Giấy")
                .district("Cầu Giấy")
                .build();

        when(userRepository.findByEmail(sampleLandlord.getEmail())).thenReturn(Optional.of(sampleLandlord));
        when(roomMapper.toEntity(eq(request), eq(sampleLandlord))).thenReturn(sampleRoom);
        when(roomRepository.save(any(Room.class))).thenReturn(sampleRoom);
        when(roomMapper.toRoomResponse(any(Room.class))).thenReturn(sampleRoomResponse);

        RoomResponse response = roomService.createRoom(request, sampleLandlord.getEmail());

        assertNotNull(response);
        assertEquals(RoomStatus.PENDING, sampleRoom.getStatus());
        verify(roomRepository, times(1)).save(sampleRoom);
    }

    @Test
    @DisplayName("Admin đăng tin thành công - được tự động duyệt APPROVED")
    void createRoom_ByAdmin_Success_ApprovedStatus() {
        CreateRoomRequest request = CreateRoomRequest.builder()
                .title("Phòng trọ Cầu Giấy")
                .price(3500000.0)
                .area(25.0)
                .address("Số 10 Cầu Giấy")
                .district("Cầu Giấy")
                .build();

        when(userRepository.findByEmail(sampleAdmin.getEmail())).thenReturn(Optional.of(sampleAdmin));
        when(roomMapper.toEntity(eq(request), eq(sampleAdmin))).thenReturn(sampleRoom);
        when(roomRepository.save(any(Room.class))).thenReturn(sampleRoom);
        when(roomMapper.toRoomResponse(any(Room.class))).thenReturn(sampleRoomResponse);

        RoomResponse response = roomService.createRoom(request, sampleAdmin.getEmail());

        assertNotNull(response);
        assertEquals(RoomStatus.APPROVED, sampleRoom.getStatus());
        verify(roomRepository, times(1)).save(sampleRoom);
    }

    @Test
    @DisplayName("Người thuê phòng (TENANT) đăng tin - Ném ngoại lệ UNAUTHORIZED")
    void createRoom_ByTenant_ThrowsUnauthorizedException() {
        CreateRoomRequest request = CreateRoomRequest.builder()
                .title("Phòng trọ trái phép")
                .price(3500000.0)
                .area(25.0)
                .address("Số 10 Cầu Giấy")
                .district("Cầu Giấy")
                .build();

        when(userRepository.findByEmail(sampleTenant.getEmail())).thenReturn(Optional.of(sampleTenant));

        AppException exception = assertThrows(AppException.class,
                () -> roomService.createRoom(request, sampleTenant.getEmail()));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Xem chi tiết phòng trọ - tự động tăng lượt xem (view_count)")
    void getRoomDetail_IncrementsViewCount() {
        UUID roomId = sampleRoom.getId();
        when(roomRepository.findByIdWithDetails(roomId)).thenReturn(Optional.of(sampleRoom));
        when(roomMapper.toRoomResponse(sampleRoom)).thenReturn(sampleRoomResponse);

        RoomResponse response = roomService.getRoomDetail(roomId, true);

        assertNotNull(response);
        verify(roomRepository, times(1)).incrementViewCount(roomId);
        assertEquals(11L, sampleRoom.getViewCount());
    }

    @Test
    @DisplayName("Chủ sở hữu chỉnh sửa phòng trọ thành công")
    void updateRoom_ByOwner_Success() {
        UUID roomId = sampleRoom.getId();
        UpdateRoomRequest request = UpdateRoomRequest.builder()
                .title("Phòng trọ Cầu Giấy Giá Rẻ")
                .price(3200000.0)
                .build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(userRepository.findByEmail(sampleLandlord.getEmail())).thenReturn(Optional.of(sampleLandlord));
        when(roomRepository.save(sampleRoom)).thenReturn(sampleRoom);
        when(roomMapper.toRoomResponse(sampleRoom)).thenReturn(sampleRoomResponse);

        RoomResponse response = roomService.updateRoom(roomId, request, sampleLandlord.getEmail());

        assertNotNull(response);
        verify(roomMapper, times(1)).updateEntity(sampleRoom, request);
        verify(roomRepository, times(1)).save(sampleRoom);
    }

    @Test
    @DisplayName("Người khác không phải chủ sở hữu cố gắng sửa tin - Ném UNAUTHORIZED")
    void updateRoom_NotOwner_ThrowsUnauthorizedException() {
        UUID roomId = sampleRoom.getId();
        UpdateRoomRequest request = UpdateRoomRequest.builder().title("Hack tin").build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(userRepository.findByEmail(sampleTenant.getEmail())).thenReturn(Optional.of(sampleTenant));

        AppException exception = assertThrows(AppException.class,
                () -> roomService.updateRoom(roomId, request, sampleTenant.getEmail()));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin duyệt tin đăng - Chuyển trạng thái sang APPROVED")
    void updateRoomStatus_ByAdmin_Success() {
        UUID roomId = sampleRoom.getId();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.save(sampleRoom)).thenReturn(sampleRoom);
        when(roomMapper.toRoomResponse(sampleRoom)).thenReturn(sampleRoomResponse);

        RoomResponse response = roomService.updateRoomStatus(roomId, RoomStatus.APPROVED);

        assertNotNull(response);
        assertEquals(RoomStatus.APPROVED, sampleRoom.getStatus());
        verify(roomRepository, times(1)).save(sampleRoom);
    }
}
