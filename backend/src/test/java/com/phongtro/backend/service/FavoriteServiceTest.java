package com.phongtro.backend.service;

import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.response.FavoriteStatusResponse;
import com.phongtro.backend.dto.response.RoomSummaryResponse;
import com.phongtro.backend.dto.response.ToggleFavoriteResponse;
import com.phongtro.backend.entity.Favorite;
import com.phongtro.backend.entity.Room;
import com.phongtro.backend.entity.Role;
import com.phongtro.backend.entity.User;
import com.phongtro.backend.entity.UserStatus;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.mapper.RoomMapper;
import com.phongtro.backend.repository.FavoriteRepository;
import com.phongtro.backend.repository.RoomRepository;
import com.phongtro.backend.repository.UserRepository;
import com.phongtro.backend.service.impl.FavoriteServiceImpl;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private FavoriteServiceImpl favoriteService;

    private User sampleUser;
    private Room sampleRoom;
    private UUID roomId;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .email("tenant@phongtro.vn")
                .fullName("Nguyễn Văn A")
                .role(Role.TENANT)
                .status(UserStatus.ACTIVE)
                .build();

        sampleRoom = Room.builder()
                .id(roomId)
                .title("Phòng trọ tiện nghi Quận 10")
                .price(4500000.0)
                .district("Quận 10")
                .city("Hồ Chí Minh")
                .build();
    }

    @Test
    @DisplayName("Toggle favorite: khi chưa yêu thích -> thêm vào yêu thích thành công")
    void toggleFavorite_WhenNotFavorited_ShouldSaveAndReturnFavoritedTrue() {
        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(favoriteRepository.findByUserAndRoom(sampleUser, sampleRoom)).thenReturn(Optional.empty());
        when(favoriteRepository.countByRoom(sampleRoom)).thenReturn(1L);

        ToggleFavoriteResponse response = favoriteService.toggleFavorite(roomId, sampleUser.getEmail());

        assertNotNull(response);
        assertTrue(response.isFavorited());
        assertEquals(roomId, response.getRoomId());
        assertEquals(1L, response.getTotalFavorites());
        verify(favoriteRepository, times(1)).save(any(Favorite.class));
        verify(favoriteRepository, never()).delete(any(Favorite.class));
    }

    @Test
    @DisplayName("Toggle favorite: khi đã yêu thích -> xóa khỏi danh sách yêu thích")
    void toggleFavorite_WhenAlreadyFavorited_ShouldDeleteAndReturnFavoritedFalse() {
        Favorite existingFavorite = Favorite.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .room(sampleRoom)
                .build();

        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(favoriteRepository.findByUserAndRoom(sampleUser, sampleRoom)).thenReturn(Optional.of(existingFavorite));
        when(favoriteRepository.countByRoom(sampleRoom)).thenReturn(0L);

        ToggleFavoriteResponse response = favoriteService.toggleFavorite(roomId, sampleUser.getEmail());

        assertNotNull(response);
        assertFalse(response.isFavorited());
        assertEquals(0L, response.getTotalFavorites());
        verify(favoriteRepository, times(1)).delete(existingFavorite);
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Toggle favorite: lỗi khi không tìm thấy người dùng")
    void toggleFavorite_WhenUserNotFound_ShouldThrowAppException() {
        when(userRepository.findByEmail("unknown@phongtro.vn")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () ->
                favoriteService.toggleFavorite(roomId, "unknown@phongtro.vn"));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Kiểm tra trạng thái yêu thích: người dùng đã đăng nhập và đã lưu phòng")
    void checkFavoriteStatus_WhenLoggedInAndFavorited_ShouldReturnTrue() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(favoriteRepository.existsByUserAndRoom(sampleUser, sampleRoom)).thenReturn(true);
        when(favoriteRepository.countByRoom(sampleRoom)).thenReturn(5L);

        FavoriteStatusResponse response = favoriteService.checkFavoriteStatus(roomId, sampleUser.getEmail());

        assertNotNull(response);
        assertTrue(response.isFavorited());
        assertEquals(5L, response.getTotalFavorites());
    }

    @Test
    @DisplayName("Kiểm tra trạng thái yêu thích: khách vãng lai (chưa đăng nhập)")
    void checkFavoriteStatus_WhenGuestUser_ShouldReturnFalseWithCount() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(favoriteRepository.countByRoom(sampleRoom)).thenReturn(3L);

        FavoriteStatusResponse response = favoriteService.checkFavoriteStatus(roomId, null);

        assertNotNull(response);
        assertFalse(response.isFavorited());
        assertEquals(3L, response.getTotalFavorites());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Lấy danh sách phòng yêu thích của tôi kèm phân trang")
    void getMyFavoriteRooms_ShouldReturnPagedResponse() {
        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));

        Page<Room> roomPage = new PageImpl<>(List.of(sampleRoom));
        when(favoriteRepository.findFavoriteRoomsByUser(eq(sampleUser), any(Pageable.class))).thenReturn(roomPage);

        RoomSummaryResponse summary = RoomSummaryResponse.builder()
                .id(roomId)
                .title(sampleRoom.getTitle())
                .price(sampleRoom.getPrice())
                .district(sampleRoom.getDistrict())
                .build();
        when(roomMapper.toRoomSummaryResponse(sampleRoom)).thenReturn(summary);

        PageResponse<RoomSummaryResponse> result = favoriteService.getMyFavoriteRooms(sampleUser.getEmail(), 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(roomId, result.getContent().get(0).getId());
    }

    @Test
    @DisplayName("Xóa phòng khỏi yêu thích thành công")
    void removeFavorite_ShouldCallDeleteByUserAndRoom() {
        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));

        favoriteService.removeFavorite(roomId, sampleUser.getEmail());

        verify(favoriteRepository, times(1)).deleteByUserAndRoom(sampleUser, sampleRoom);
    }

    @Test
    @DisplayName("Lấy danh sách ID các phòng trọ yêu thích của người dùng")
    void getMyFavoriteRoomIds_ShouldReturnListOfUUIDs() {
        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(favoriteRepository.findFavoriteRoomIdsByUser(sampleUser)).thenReturn(List.of(roomId));

        List<UUID> result = favoriteService.getMyFavoriteRoomIds(sampleUser.getEmail());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(roomId, result.get(0));
    }
}
