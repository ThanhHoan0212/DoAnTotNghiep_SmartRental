package com.phongtro.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu cập nhật thông tin cá nhân người dùng")
public class UpdateUserRequest {

    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự")
    @Schema(description = "Họ và tên người dùng", example = "Nguyễn Văn B")
    private String fullName;

    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam")
    @Schema(description = "Số điện thoại liên hệ", example = "0912345678")
    private String phone;

    @Size(max = 500, message = "Đường dẫn ảnh đại diện không được vượt quá 500 ký tự")
    @Schema(description = "URL ảnh đại diện", example = "https://example.com/avatar.jpg")
    private String avatarUrl;
}
