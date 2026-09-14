package com.phongtro.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả thao tác lưu / hủy lưu phòng trọ yêu thích")
public class ToggleFavoriteResponse {

    @Schema(description = "ID phòng trọ", example = "b0000000-0000-0000-0000-000000000001")
    private UUID roomId;

    @Schema(description = "Trạng thái sau thao tác: true = đã lưu yêu thích, false = đã hủy lưu", example = "true")
    @JsonProperty("isFavorited")
    private boolean isFavorited;

    @Schema(description = "Thông báo kết quả thao tác", example = "Đã thêm vào danh sách phòng yêu thích")
    private String message;

    @Schema(description = "Tổng số lượt yêu thích hiện tại của phòng này", example = "12")
    private long totalFavorites;

    @JsonProperty("isFavorited")
    public boolean isFavorited() {
        return isFavorited;
    }

    @JsonProperty("favorited")
    public boolean getFavorited() {
        return isFavorited;
    }
}
