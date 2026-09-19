package com.phongtro.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.fpt-ai")
public class FptAiProperties {

    private String apiKey = "";
    private String ocrUrl = "https://api.fpt.ai/vision/idr/vnm";
    private String facematchUrl = "https://api.fpt.ai/vision/ekyc/facematch/v4";
    private Double confidenceThreshold = 85.0;
    private boolean mockEnabled = false;

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
