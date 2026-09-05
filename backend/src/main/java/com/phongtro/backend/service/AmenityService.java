package com.phongtro.backend.service;

import com.phongtro.backend.dto.request.AmenityRequest;
import com.phongtro.backend.dto.response.AmenityResponse;

import java.util.List;

public interface AmenityService {

    List<AmenityResponse> getAllAmenities();

    AmenityResponse createAmenity(AmenityRequest request);
}
