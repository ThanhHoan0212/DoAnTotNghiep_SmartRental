package com.phongtro.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckRequest {
    private String title;
    private String description;
    private Double price;
    private Double area;
    private String district;
    private String address;

    @JsonProperty("landlord_id")
    private Long landlordId;

    @JsonProperty("room_type")
    private String roomType;
}
