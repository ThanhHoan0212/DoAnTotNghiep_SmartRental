package com.phongtro.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.vnpt-smartca")
public class VnptSmartCaProperties {

    private String baseUrl = "https://rmgateway.vnptit.vn/sca/sp769";
    private String authUrl = "https://rmgateway-mobile.vnptit.vn/auth/token";
    private String partnerInitUrl = "https://rmgateway.vnptit.vn/vi/thirdpartyapi/sdk/partner_init";
    private String clientId = "44a6-639267272520259699.apps.smartcaapi.com";
    private String clientSecret = "M2FkN2Q5NGQ-NjQ5Ni00NGE2";
    private String spId = "44a6-639267272520259699.apps.smartcaapi.com";
    private String spPassword = "M2FkN2Q5NGQ-NjQ5Ni00NGE2";
    private boolean sandboxMode = true;

    public boolean hasCredentials() {
        return clientId != null && !clientId.trim().isEmpty() &&
               clientSecret != null && !clientSecret.trim().isEmpty();
    }
}
