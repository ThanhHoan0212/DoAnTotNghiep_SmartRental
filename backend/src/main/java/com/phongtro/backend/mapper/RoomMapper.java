package com.phongtro.backend.mapper;

import com.phongtro.backend.dto.request.CreateRoomRequest;
import com.phongtro.backend.dto.request.UpdateRoomRequest;
import com.phongtro.backend.dto.response.AmenityResponse;
import com.phongtro.backend.dto.response.RoomImageResponse;
import com.phongtro.backend.dto.response.RoomResponse;
import com.phongtro.backend.dto.response.RoomSummaryResponse;
import com.phongtro.backend.entity.Amenity;
import com.phongtro.backend.entity.Room;
import com.phongtro.backend.entity.RoomImage;
import com.phongtro.backend.entity.RoomStatus;
import com.phongtro.backend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RoomMapper {

    private final UserMapper userMapper;

    public AmenityResponse toAmenityResponse(Amenity amenity) {
        if (amenity == null) return null;
        return AmenityResponse.builder()
                .id(amenity.getId())
                .name(amenity.getName())
                .icon(amenity.getIcon())
                .build();
    }

    public RoomImageResponse toRoomImageResponse(RoomImage image) {
        if (image == null) return null;
        return RoomImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .isPrimary(image.isPrimary())
                .build();
    }

    public RoomResponse toRoomResponse(Room room) {
        if (room == null) return null;

        List<RoomImageResponse> imageResponses = room.getImages() != null
                ? room.getImages().stream().map(this::toRoomImageResponse).collect(Collectors.toList())
                : Collections.emptyList();

        List<AmenityResponse> amenityResponses = room.getAmenities() != null
                ? room.getAmenities().stream().map(this::toAmenityResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return RoomResponse.builder()
                .id(room.getId())
                .landlord(userMapper.toUserResponse(room.getLandlord()))
                .title(room.getTitle())
                .description(room.getDescription())
                .price(room.getPrice())
                .area(room.getArea())
                .address(room.getAddress())
                .ward(room.getWard())
                .district(room.getDistrict())
                .city(room.getCity())
                .latitude(room.getLatitude())
                .longitude(room.getLongitude())
                .status(room.getStatus())
                .viewCount(room.getViewCount())
                .images(imageResponses)
                .amenities(amenityResponses)
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    public RoomSummaryResponse toRoomSummaryResponse(Room room) {
        if (room == null) return null;

        String primaryImageUrl = null;
        if (room.getImages() != null && !room.getImages().isEmpty()) {
            primaryImageUrl = room.getImages().stream()
                    .filter(RoomImage::isPrimary)
                    .map(RoomImage::getImageUrl)
                    .findFirst()
                    .orElse(room.getImages().get(0).getImageUrl());
        }

        return RoomSummaryResponse.builder()
                .id(room.getId())
                .title(room.getTitle())
                .price(room.getPrice())
                .area(room.getArea())
                .address(room.getAddress())
                .district(room.getDistrict())
                .city(room.getCity())
                .primaryImageUrl(primaryImageUrl)
                .status(room.getStatus())
                .viewCount(room.getViewCount())
                .createdAt(room.getCreatedAt())
                .build();
    }

    public Room toEntity(CreateRoomRequest request, User landlord) {
        if (request == null) return null;

        return Room.builder()
                .landlord(landlord)
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .price(request.getPrice())
                .area(request.getArea())
                .address(request.getAddress().trim())
                .ward(request.getWard() != null ? request.getWard().trim() : null)
                .district(request.getDistrict().trim())
                .city(request.getCity() != null ? request.getCity().trim() : "Hà Nội")
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(RoomStatus.PENDING) // Mặc định tin đăng mới ở trạng thái chờ duyệt PENDING
                .viewCount(0L)
                .build();
    }

    public void updateEntity(Room room, UpdateRoomRequest request) {
        if (room == null || request == null) return;

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            room.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            room.setDescription(request.getDescription().trim());
        }
        if (request.getPrice() != null) {
            room.setPrice(request.getPrice());
        }
        if (request.getArea() != null) {
            room.setArea(request.getArea());
        }
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            room.setAddress(request.getAddress().trim());
        }
        if (request.getWard() != null) {
            room.setWard(request.getWard().trim());
        }
        if (request.getDistrict() != null && !request.getDistrict().isBlank()) {
            room.setDistrict(request.getDistrict().trim());
        }
        if (request.getCity() != null && !request.getCity().isBlank()) {
            room.setCity(request.getCity().trim());
        }
        if (request.getLatitude() != null) {
            room.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            room.setLongitude(request.getLongitude());
        }
        if (request.getStatus() != null) {
            room.setStatus(request.getStatus());
        }
    }
}
