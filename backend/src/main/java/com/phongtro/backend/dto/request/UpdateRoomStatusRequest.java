package com.phongtro.backend.dto.request;

import com.phongtro.backend.entity.RoomStatus;
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
@Schema(description = "Yêu cầu thay đổi trạng thái tin đăng (Admin duyệt / từ chối / ẩn)")
public class UpdateRoomStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    @Schema(description = "Trạng thái mới (APPROVED, REJECTED, HIDDEN)", example = "APPROVED")
    private RoomStatus status;
}
