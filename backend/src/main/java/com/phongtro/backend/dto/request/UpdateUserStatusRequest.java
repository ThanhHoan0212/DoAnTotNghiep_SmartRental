package com.phongtro.backend.dto.request;

import com.phongtro.backend.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu cập nhật trạng thái người dùng (Admin)")
public class UpdateUserStatusRequest {

    @NotNull(message = "Trạng thái người dùng không được để trống")
    @Schema(description = "Trạng thái mới (ACTIVE, INACTIVE, BANNED)", example = "BANNED")
    private UserStatus status;
}
