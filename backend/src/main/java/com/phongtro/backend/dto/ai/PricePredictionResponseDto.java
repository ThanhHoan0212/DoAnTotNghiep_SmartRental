package com.phongtro.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PricePredictionResponseDto {

    private String district;

    @JsonProperty("room_type")
    private String roomType;

    @JsonProperty("current_avg_price")
    private Double currentAvgPrice;

    @JsonProperty("predicted_next_month")
    private Double predictedNextMonth;

    private String trend;

    @JsonProperty("confidence_interval")
    private List<Double> confidenceInterval;

    @JsonProperty("change_percentage")
    private Double changePercentage;

    private List<String> factors;
}
