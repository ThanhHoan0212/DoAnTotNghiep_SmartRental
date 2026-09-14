package com.phongtro.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả phân tích câu truy vấn tự nhiên của AI")
public class ParsedQueryResponse {

    @Schema(description = "Câu truy vấn gốc của người dùng", example = "tìm phòng trọ khép kín ở Bình Thạnh dưới 4 triệu có điều hòa và máy giặt")
    private String originalQuery;

    @Schema(description = "Từ khóa tìm kiếm nhận diện được", example = "khép kín")
    private String keyword;

    @Schema(description = "Quận / Huyện nhận diện được", example = "Bình Thạnh")
    private String district;

    @Schema(description = "Mức giá tối thiểu nhận diện được", example = "0")
    private Double minPrice;

    @Schema(description = "Mức giá tối đa nhận diện được", example = "4000000")
    private Double maxPrice;

    @Schema(description = "Diện tích tối thiểu nhận diện được", example = "20.0")
    private Double minArea;

    @Schema(description = "Diện tích tối đa nhận diện được", example = "40.0")
    private Double maxArea;

    @Schema(description = "Tên các tiện ích được nhận diện", example = "[\"Điều hòa nhiệt độ\", \"Máy giặt chung\"]")
    @Builder.Default
    private List<String> detectedAmenities = new ArrayList<>();

    @Schema(description = "Danh sách ID tiện ích khớp trong cơ sở dữ liệu", example = "[2, 6]")
    @Builder.Default
    private List<Long> amenityIds = new ArrayList<>();

    @Schema(description = "Nguồn phân tích (AI_SERVICE hoặc RULE_BASED_FALLBACK)", example = "AI_SERVICE")
    private String source;
}
