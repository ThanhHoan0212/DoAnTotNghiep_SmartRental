package com.phongtro.backend.dto.ai;

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
public class PersonalizedRecommendRequest {

    @JsonProperty("user_id")
    private Object userId;

    @JsonProperty("viewed_room_ids")
    private List<Object> viewedRoomIds;

    @JsonProperty("favorite_room_ids")
    private List<Object> favoriteRoomIds;

    @JsonProperty("preferred_districts")
    private List<String> preferredDistricts;

    @JsonProperty("target_room_id")
    private Object targetRoomId;

    private Integer limit;
}

