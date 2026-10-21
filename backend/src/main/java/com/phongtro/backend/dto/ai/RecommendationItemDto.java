package com.phongtro.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationItemDto {

    @JsonProperty("room_id")
    private Object roomId;

    private String title;

    private String district;

    private Double price;

    private Double area;

    private Double score;

    private String reason;

    @JsonProperty("match_factors")
    private Map<String, Object> matchFactors;
}

