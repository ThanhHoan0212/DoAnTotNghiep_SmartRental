package com.phongtro.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RespondClosureRequest {
    @NotNull
    private Boolean accepted;
    @Size(max = 2000)
    private String reason;
}
