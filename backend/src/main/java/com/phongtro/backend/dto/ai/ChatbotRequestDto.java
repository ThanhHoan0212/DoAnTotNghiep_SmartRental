package com.phongtro.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequestDto {

    @JsonProperty("user_id")
    private Long userId;

    @NotBlank(message = "Session ID không được để trống")
    @com.fasterxml.jackson.annotation.JsonAlias({"sessionId", "session_id"})
    @JsonProperty("session_id")
    private String sessionId;

    @NotBlank(message = "Tin nhắn không được để trống")
    private String message;

    private ChatbotContextDto context;
}
