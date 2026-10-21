package com.phongtro.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendationResponse {

    @JsonProperty("user_id")
    private Object userId;

    @JsonProperty("recommended_room_ids")
    private List<Object> recommendedRoomIds;

    @JsonAlias({"items", "recommendations"})
    @JsonProperty("items")
    private List<RecommendationItemDto> recommendations;

    private String strategy;
}

