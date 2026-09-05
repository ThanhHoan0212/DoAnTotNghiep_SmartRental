package com.phongtro.backend.service.impl;

import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.request.CreateRoomRequest;
import com.phongtro.backend.dto.request.RoomImageRequest;
import com.phongtro.backend.dto.request.UpdateRoomRequest;
import com.phongtro.backend.dto.response.RoomResponse;
import com.phongtro.backend.dto.response.RoomSummaryResponse;
import com.phongtro.backend.entity.*;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.mapper.RoomMapper;
import com.phongtro.backend.repository.AmenityRepository;
import com.phongtro.backend.repository.RoomRepository;
import com.phongtro.backend.repository.UserRepository;
import com.phongtro.backend.service.RoomService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final AmenityRepository amenityRepository;
    private final RoomMapper roomMapper;

    @Override
    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request, String landlordEmail) {
        User landlord = userRepository.findByEmail(landlordEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (landlord.getRole() == Role.TENANT) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Tài khoản người thuê không có quyền đăng tin phòng trọ");
        }

        Room room = roomMapper.toEntity(request, landlord);

        // Xử lý tiện ích (Many-to-Many)
        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            List<Amenity> amenities = amenityRepository.findAllById(request.getAmenityIds());
            room.setAmenities(new HashSet<>(amenities));
        }

        // Xử lý hình ảnh (One-to-Many)
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            boolean hasPrimary = false;
            for (RoomImageRequest imgReq : request.getImages()) {
                boolean isPrimary = imgReq.isPrimary() && !hasPrimary;
                if (isPrimary) hasPrimary = true;

                RoomImage roomImage = RoomImage.builder()
                        .imageUrl(imgReq.getImageUrl().trim())
                        .isPrimary(isPrimary)
                        .build();
                room.addImage(roomImage);
            }
            // Nếu chưa có ảnh nào được đánh dấu là primary thì gán ảnh đầu tiên làm primary
            if (!hasPrimary && !room.getImages().isEmpty()) {
                room.getImages().get(0).setPrimary(true);
            }
        }

        // Nếu là ADMIN đăng tin thì duyệt luôn, nếu là LANDLORD thì để PENDING chờ duyệt
        if (landlord.getRole() == Role.ADMIN) {
            room.setStatus(RoomStatus.APPROVED);
        } else {
            room.setStatus(RoomStatus.PENDING);
        }

        Room savedRoom = roomRepository.save(room);
        log.info("User [{}] created room listing [{}] with status [{}]", landlordEmail, savedRoom.getId(), savedRoom.getStatus());
        return roomMapper.toRoomResponse(savedRoom);
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(UUID roomId, UpdateRoomRequest request, String userEmail) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy phòng trọ"));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra quyền sở hữu (chỉ chính chủ nhà hoặc ADMIN mới được sửa)
        if (!room.getLandlord().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền chỉnh sửa tin đăng này");
        }

        roomMapper.updateEntity(room, request);

        // Cập nhật tiện ích nếu có thay đổi
        if (request.getAmenityIds() != null) {
            List<Amenity> amenities = amenityRepository.findAllById(request.getAmenityIds());
            room.setAmenities(new HashSet<>(amenities));
        }

        // Cập nhật hình ảnh nếu có gửi lên
        if (request.getImages() != null) {
            room.clearImages();
            boolean hasPrimary = false;
            for (RoomImageRequest imgReq : request.getImages()) {
                boolean isPrimary = imgReq.isPrimary() && !hasPrimary;
                if (isPrimary) hasPrimary = true;

                RoomImage roomImage = RoomImage.builder()
                        .imageUrl(imgReq.getImageUrl().trim())
                        .isPrimary(isPrimary)
                        .build();
                room.addImage(roomImage);
            }
            if (!hasPrimary && !room.getImages().isEmpty()) {
                room.getImages().get(0).setPrimary(true);
            }
        }

        Room updated = roomRepository.save(room);
        log.info("User [{}] updated room listing [{}]", userEmail, roomId);
        return roomMapper.toRoomResponse(updated);
    }

    @Override
    @Transactional
    public void deleteRoom(UUID roomId, String userEmail) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy phòng trọ"));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra quyền sở hữu
        if (!room.getLandlord().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền xóa tin đăng này");
        }

        roomRepository.delete(room);
        log.info("User [{}] deleted room listing [{}]", userEmail, roomId);
    }

    @Override
    @Transactional
    public RoomResponse getRoomDetail(UUID roomId, boolean incrementView) {
        Room room = roomRepository.findByIdWithDetails(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy phòng trọ"));

        if (incrementView) {
            roomRepository.incrementViewCount(roomId);
            room.setViewCount(room.getViewCount() + 1);
        }

        return roomMapper.toRoomResponse(room);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoomSummaryResponse> getApprovedRooms(
            int page,
            int size,
            String district,
            Double minPrice,
            Double maxPrice,
            Long amenityId
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<Room> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Chỉ lấy các tin đã được duyệt
            predicates.add(cb.equal(root.get("status"), RoomStatus.APPROVED));

            if (district != null && !district.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("district")), "%" + district.trim().toLowerCase() + "%"));
            }

            if (minPrice != null && minPrice > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null && maxPrice > 0) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (amenityId != null) {
                Join<Room, Amenity> amenityJoin = root.join("amenities");
                predicates.add(cb.equal(amenityJoin.get("id"), amenityId));
            }

            // Tránh bản ghi duplicate khi join với many-to-many
            if (query != null) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Room> roomPage = roomRepository.findAll(spec, pageable);
        Page<RoomSummaryResponse> dtoPage = roomPage.map(roomMapper::toRoomSummaryResponse);
        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoomSummaryResponse> getMyRooms(String landlordEmail, int page, int size, RoomStatus status) {
        User landlord = userRepository.findByEmail(landlordEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Room> roomPage;

        if (status != null) {
            roomPage = roomRepository.findByLandlordAndStatus(landlord, status, pageable);
        } else {
            roomPage = roomRepository.findByLandlord(landlord, pageable);
        }

        Page<RoomSummaryResponse> dtoPage = roomPage.map(roomMapper::toRoomSummaryResponse);
        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoomSummaryResponse> getAllRoomsForAdmin(int page, int size, RoomStatus status, String district) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<Room> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (district != null && !district.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("district")), "%" + district.trim().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Room> roomPage = roomRepository.findAll(spec, pageable);
        Page<RoomSummaryResponse> dtoPage = roomPage.map(roomMapper::toRoomSummaryResponse);
        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional
    public RoomResponse updateRoomStatus(UUID roomId, RoomStatus status) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy phòng trọ"));

        room.setStatus(status);
        Room updated = roomRepository.save(room);
        log.info("Admin updated room [{}] status to [{}]", roomId, status);
        return roomMapper.toRoomResponse(updated);
    }
}
