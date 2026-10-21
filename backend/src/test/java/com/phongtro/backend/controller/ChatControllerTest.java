package com.phongtro.backend.controller;

import com.phongtro.backend.config.CorsConfig;
import com.phongtro.backend.config.SecurityConfig;
import com.phongtro.backend.security.*;
import com.phongtro.backend.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class ChatControllerTest {
    @Autowired MockMvc mvc;
    @Autowired RequestMappingHandlerMapping mappings;
    @MockBean ChatService chat;
    @MockBean JwtTokenProvider tokens;
    @MockBean UserDetailsServiceImpl details;

    @Test
    void accountSearchAndDirectCreationAreUnavailable() {
        var paths = mappings.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream()).toList();
        assertFalse(paths.contains("/api/v1/chat/contacts"));
        assertFalse(paths.contains("/api/v1/chat/users/{userId}"));
        assertTrue(paths.contains("/api/v1/chat/rooms/{roomId}"));
        verifyNoInteractions(chat);
    }

    @Test @WithMockUser(username = "account@test.vn", roles = "ADMIN")
    void authenticatedAccountCanStartThroughRoom() throws Exception {
        UUID roomId = UUID.randomUUID();
        mvc.perform(post("/api/v1/chat/rooms/" + roomId)).andExpect(status().isOk());
        verify(chat).start(roomId, "account@test.vn");
    }

    @Test
    void anonymousCannotStartThroughRoom() throws Exception {
        mvc.perform(post("/api/v1/chat/rooms/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        verifyNoInteractions(chat);
    }

    @Test @WithMockUser(username = "account@test.vn")
    void uploadUsesAuthenticatedAccount() throws Exception {
        UUID id = UUID.randomUUID();
        var file = new org.springframework.mock.web.MockMultipartFile("image", "room.png", "image/png", new byte[]{1, 2});
        mvc.perform(multipart("/api/v1/chat/conversations/" + id + "/images").file(file).param("caption", "Ảnh phòng"))
                .andExpect(status().isOk());
        verify(chat).sendImage(eq(id), any(), eq("Ảnh phòng"), eq("account@test.vn"));
    }

    @Test
    void anonymousCannotUploadImages() throws Exception {
        mvc.perform(multipart("/api/v1/chat/conversations/" + UUID.randomUUID() + "/images")
                .file(new org.springframework.mock.web.MockMultipartFile("image", new byte[]{1})))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(chat);
    }

    @Test @WithMockUser(username = "account@test.vn")
    void imageResponseIsNotPubliclyCached() throws Exception {
        UUID id = UUID.randomUUID(), messageId = UUID.randomUUID();
        mvc.perform(get("/api/v1/chat/conversations/" + id + "/messages/" + messageId + "/image"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        verify(chat).image(id, messageId, "account@test.vn");
    }

    @Test @WithMockUser(username = "account@test.vn")
    void sharedImagesUsesAuthenticatedAccountAndRequestedPage() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(get("/api/v1/chat/conversations/" + id + "/images").param("page", "2"))
                .andExpect(status().isOk());
        verify(chat).sharedImages(id, 2, "account@test.vn");
    }

    @Test @WithMockUser(username = "account@test.vn")
    void videoSupportsAuthorizedByteRanges() throws Exception {
        UUID id = UUID.randomUUID(), messageId = UUID.randomUUID();
        when(chat.video(id, messageId, "account@test.vn")).thenReturn(new com.phongtro.backend.dto.ChatDtos.ImageData("video/mp4", new byte[]{0, 1, 2, 3, 4, 5}));
        mvc.perform(get("/api/v1/chat/conversations/" + id + "/messages/" + messageId + "/video").header("Range", "bytes=1-3"))
                .andExpect(status().isPartialContent()).andExpect(header().string("Content-Range", "bytes 1-3/6"))
                .andExpect(content().bytes(new byte[]{1, 2, 3})).andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test void anonymousCannotUploadVideo() throws Exception {
        mvc.perform(multipart("/api/v1/chat/conversations/" + UUID.randomUUID() + "/videos")
                .file(new org.springframework.mock.web.MockMultipartFile("video", new byte[]{1})))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(chat);
    }
}
