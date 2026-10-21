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
public class UserInteractionSyncRequest {

    @JsonProperty("user_id")
    private Object userId;

    @JsonProperty("room_id")
    private Object roomId;

    @JsonProperty("interaction_type")
    private String interactionType;

    private Map<String, Object> metadata;
}

