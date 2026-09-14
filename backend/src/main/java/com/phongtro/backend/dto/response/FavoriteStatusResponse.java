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
@Schema(description = "Trạng thái yêu thích của một phòng trọ đối với người dùng hiện tại")
public class FavoriteStatusResponse {

    @Schema(description = "ID phòng trọ", example = "b0000000-0000-0000-0000-000000000001")
    private UUID roomId;

    @Schema(description = "Người dùng hiện tại đã lưu phòng này hay chưa", example = "true")
    @JsonProperty("isFavorited")
    private boolean isFavorited;

    @Schema(description = "Tổng số người đã lưu yêu thích phòng này", example = "12")
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
