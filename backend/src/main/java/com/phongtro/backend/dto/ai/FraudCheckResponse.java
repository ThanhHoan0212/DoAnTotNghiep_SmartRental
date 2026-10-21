package com.phongtro.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FraudCheckResponse {

    @JsonProperty("fraud_score")
    private Double fraudScore;

    @JsonProperty("risk_level")
    private String riskLevel;

    @JsonProperty("is_fraud")
    private Boolean isFraud;

    private String recommendation;

    private List<String> reasons;

    private Map<String, Object> details;
}
