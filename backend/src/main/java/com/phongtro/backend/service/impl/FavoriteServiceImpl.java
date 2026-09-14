package com.phongtro.backend.service.impl;

import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.response.FavoriteStatusResponse;
import com.phongtro.backend.dto.response.RoomSummaryResponse;
import com.phongtro.backend.dto.response.ToggleFavoriteResponse;
import com.phongtro.backend.entity.Favorite;
import com.phongtro.backend.entity.Room;
import com.phongtro.backend.entity.User;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.mapper.RoomMapper;
import com.phongtro.backend.repository.FavoriteRepository;
import com.phongtro.backend.repository.RoomRepository;
import com.phongtro.backend.repository.UserRepository;
import com.phongtro.backend.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;

    @Override
    @Transactional
    public ToggleFavoriteResponse toggleFavorite(UUID roomId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Room room = getRoomById(roomId);

        Optional<Favorite> existingFavorite = favoriteRepository.findByUserAndRoom(user, room);
        boolean isFavorited;
        String message;

        if (existingFavorite.isPresent()) {
            favoriteRepository.delete(existingFavorite.get());
            isFavorited = false;
            message = "Đã xóa phòng trọ khỏi danh sách yêu thích";
            log.info("User [{}] un-favorited room [{}]", userEmail, roomId);
        } else {
            Favorite favorite = Favorite.builder()
                    .user(user)
                    .room(room)
                    .build();
            favoriteRepository.save(favorite);
            isFavorited = true;
            message = "Đã lưu phòng trọ vào danh sách yêu thích";
            log.info("User [{}] favorited room [{}]", userEmail, roomId);
        }

        long totalFavorites = favoriteRepository.countByRoom(room);

        return ToggleFavoriteResponse.builder()
                .roomId(roomId)
                .isFavorited(isFavorited)
                .message(message)
                .totalFavorites(totalFavorites)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FavoriteStatusResponse checkFavoriteStatus(UUID roomId, String userEmail) {
        Room room = getRoomById(roomId);
        boolean isFavorited = false;

        if (userEmail != null && !userEmail.isBlank()) {
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isPresent()) {
                isFavorited = favoriteRepository.existsByUserAndRoom(userOpt.get(), room);
            }
        }

        long totalFavorites = favoriteRepository.countByRoom(room);

        return FavoriteStatusResponse.builder()
                .roomId(roomId)
                .isFavorited(isFavorited)
                .totalFavorites(totalFavorites)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoomSummaryResponse> getMyFavoriteRooms(String userEmail, int page, int size) {
        User user = getUserByEmail(userEmail);

        int pageNumber = Math.max(0, page);
        int pageSize = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<Room> favoriteRooms = favoriteRepository.findFavoriteRoomsByUser(user, pageable);
        Page<RoomSummaryResponse> responsePage = favoriteRooms.map(roomMapper::toRoomSummaryResponse);

        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<UUID> getMyFavoriteRoomIds(String userEmail) {
        User user = getUserByEmail(userEmail);
        return favoriteRepository.findFavoriteRoomIdsByUser(user);
    }

    @Override
    @Transactional
    public void removeFavorite(UUID roomId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Room room = getRoomById(roomId);

        favoriteRepository.deleteByUserAndRoom(user, room);
        log.info("User [{}] explicitly removed room [{}] from favorites", userEmail, roomId);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Room getRoomById(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
