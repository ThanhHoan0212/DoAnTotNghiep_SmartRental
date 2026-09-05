package com.phongtro.backend.service.impl;

import com.phongtro.backend.dto.request.AmenityRequest;
import com.phongtro.backend.dto.response.AmenityResponse;
import com.phongtro.backend.entity.Amenity;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.mapper.RoomMapper;
import com.phongtro.backend.repository.AmenityRepository;
import com.phongtro.backend.service.AmenityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmenityServiceImpl implements AmenityService {

    private final AmenityRepository amenityRepository;
    private final RoomMapper roomMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AmenityResponse> getAllAmenities() {
        return amenityRepository.findAll().stream()
                .map(roomMapper::toAmenityResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AmenityResponse createAmenity(AmenityRequest request) {
        if (amenityRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Tiện ích này đã tồn tại trong hệ thống");
        }

        Amenity amenity = Amenity.builder()
                .name(request.getName().trim())
                .icon(request.getIcon() != null ? request.getIcon().trim() : null)
                .build();

        Amenity saved = amenityRepository.save(amenity);
        log.info("Admin created new amenity: [{}]", saved.getName());
        return roomMapper.toAmenityResponse(saved);
    }
}
