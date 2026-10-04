package com.phongtro.backend.dto.request;

import com.phongtro.backend.entity.ContractClosure;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateClosureRequest {
    @NotBlank @Size(max = 2000)
    private String reason;
    private LocalDate requestedEndDate;
    @NotNull
    private ContractClosure.Refund refundType;
    @NotNull @PositiveOrZero
    private Double refundAmount;
    @NotBlank @Size(max = 2000)
    private String settlementNote;
}
