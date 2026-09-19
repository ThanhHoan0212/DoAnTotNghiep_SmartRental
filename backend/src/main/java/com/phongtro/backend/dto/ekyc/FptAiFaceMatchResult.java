package com.phongtro.backend.dto.ekyc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FptAiFaceMatchResult {

    private Double similarity;
    private Boolean isMatch;
    private String rawJson;
}
