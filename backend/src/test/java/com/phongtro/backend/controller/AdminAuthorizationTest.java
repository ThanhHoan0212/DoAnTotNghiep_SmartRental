package com.phongtro.backend.controller;

import com.phongtro.backend.config.CorsConfig;
import com.phongtro.backend.config.SecurityConfig;
import com.phongtro.backend.security.*;
import com.phongtro.backend.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AdminController.class, SupportController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class AdminAuthorizationTest {
    @Autowired MockMvc mvc;
    @MockBean AdminOperationsService operations;
    @MockBean AdminAnalyticsService analytics;
    @MockBean RoomService roomService;
    @MockBean JwtTokenProvider tokens;
    @MockBean UserDetailsServiceImpl details;

    @Test void anonymousCannotReadAdminData() throws Exception {
        mvc.perform(get("/api/v1/admin/analytics")).andExpect(status().isUnauthorized());
    }
    @Test @WithMockUser(roles = "TENANT") void tenantCannotReadUsersOrReports() throws Exception {
        mvc.perform(get("/api/v1/admin/users")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/analytics")).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(roles = "LANDLORD") void landlordCannotReadComplaints() throws Exception {
        mvc.perform(get("/api/v1/admin/complaints")).andExpect(status().isForbidden());

    }
    @Test @WithMockUser(roles = "ADMIN") void adminCanReadReports() throws Exception {
        mvc.perform(get("/api/v1/admin/analytics")).andExpect(status().isOk());
    }
    @Test void anonymousCannotSubmitComplaints() throws Exception {
        mvc.perform(post("/api/v1/complaints").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

}
