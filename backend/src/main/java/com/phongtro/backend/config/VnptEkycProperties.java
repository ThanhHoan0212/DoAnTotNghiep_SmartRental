package com.phongtro.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.vnpt-ekyc")
public class VnptEkycProperties {

    private String baseUrl = "https://api.idg.vnpt.vn";
    private String accessToken = "";
    private String tokenId = "";
    private String tokenKey = "";
    private String addFileUrl = "https://api.idg.vnpt.vn/file-service/v1/addFile";
    private String ocrUrl = "https://api.idg.vnpt.vn/ai/v1/ocr/id";
    private String facematchUrl = "https://api.idg.vnpt.vn/ai/v1/face/compare";
    private Double confidenceThreshold = 85.0;
    private boolean mockEnabled = false;

    public boolean hasCredentials() {
        return (accessToken != null && !accessToken.trim().isEmpty())
                || (tokenKey != null && !tokenKey.trim().isEmpty());
    }
}
