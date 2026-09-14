package com.phongtro.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu tìm kiếm thông minh bằng câu tự nhiên (NLP / AI Search)")
public class NlpSearchRequest {

    @NotBlank(message = "Câu truy vấn tìm kiếm không được để trống")
    @Schema(description = "Câu tìm kiếm tự nhiên của người dùng", example = "tìm phòng trọ khép kín ở Bình Thạnh dưới 4 triệu có điều hòa và máy giặt")
    private String queryText;

    @Schema(description = "Số trang (bắt đầu từ 0)", example = "0", defaultValue = "0")
    @Builder.Default
    private int page = 0;

    @Schema(description = "Số lượng bản ghi mỗi trang", example = "10", defaultValue = "10")
    @Builder.Default
    private int size = 10;
}
