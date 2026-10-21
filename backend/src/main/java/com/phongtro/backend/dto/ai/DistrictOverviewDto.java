package com.phongtro.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistrictOverviewDto {

    private String district;

    @JsonProperty("avg_price")
    private Double avgPrice;

    @JsonProperty("min_price")
    private Double minPrice;

    @JsonProperty("max_price")
    private Double maxPrice;

    @JsonProperty("avg_price_per_m2")
    private Double avgPricePerM2;

    private String trend;

    @JsonProperty("sample_count")
    private Integer sampleCount;
}
