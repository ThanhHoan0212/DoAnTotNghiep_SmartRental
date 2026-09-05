package com.phongtro.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hệ Thống Tìm Kiếm Phòng Trọ Thông Minh - API Docs")
                        .description("Tài liệu đặc tả RESTful API cho Đồ án Tốt nghiệp: " +
                                "\"Phát triển hệ thống tìm kiếm phòng trọ thông minh: " +
                                "Tích hợp AI phát hiện gian lận và dự đoán xu hướng giá thuê\"")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Đội ngũ phát triển Đồ án")
                                .email("contact@phongtro.vn"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập Access Token (JWT) theo định dạng: Bearer {token}")));
    }
}
