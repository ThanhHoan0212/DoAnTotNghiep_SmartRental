package com.phongtro.backend.controller;

import com.phongtro.backend.common.ApiResponse;
import com.phongtro.backend.dto.ChatDtos.*;
import com.phongtro.backend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/chat") @RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChatController {
    private final ChatService chat;
    @PostMapping("/conversations/{id}/messages/{messageId}/recall")
    public ApiResponse<Message> recall(@PathVariable UUID id, @PathVariable UUID messageId, Authentication auth) {
        return ApiResponse.success(chat.recall(id, messageId, auth.getName()));
    }
    @PostMapping("/conversations/{id}/messages/{messageId}/reaction")
    public ApiResponse<Message> react(@PathVariable UUID id, @PathVariable UUID messageId,
            @Valid @RequestBody ReactionRequest body, Authentication auth) {
        return ApiResponse.success(chat.react(id, messageId, body.emoji(), auth.getName()));
    }
    @GetMapping("/conversations/{id}/media")
    public ApiResponse<ImagePage> sharedMedia(@PathVariable UUID id, @RequestParam(defaultValue = "0") int page, Authentication auth) {
        return ApiResponse.success(chat.sharedMedia(id, page, auth.getName()));
    }
    @PostMapping(value = "/conversations/{id}/videos", consumes = "multipart/form-data")
    public ApiResponse<Message> sendVideo(@PathVariable UUID id,
            @RequestParam("video") org.springframework.web.multipart.MultipartFile video,
            @RequestParam(defaultValue = "") String caption, Authentication auth) {
        return ApiResponse.success(chat.sendVideo(id, video, caption, auth.getName()));
    }
    @GetMapping("/conversations/{id}/messages/{messageId}/video")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> video(@PathVariable UUID id,
            @PathVariable UUID messageId, Authentication auth) {
        ImageData media = chat.video(id, messageId, auth.getName());
        return org.springframework.http.ResponseEntity.ok().contentType(org.springframework.http.MediaType.valueOf(media.contentType()))
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(new org.springframework.core.io.ByteArrayResource(media.data()));
    }
    @GetMapping("/conversations/{id}/images")
    public ApiResponse<ImagePage> sharedImages(@PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page, Authentication auth) {
        return ApiResponse.success(chat.sharedImages(id, page, auth.getName()));
    }
    @PostMapping(value = "/conversations/{id}/images", consumes = "multipart/form-data")
    public ApiResponse<Message> sendImage(@PathVariable UUID id,
            @RequestParam("image") org.springframework.web.multipart.MultipartFile image,
            @RequestParam(defaultValue = "") String caption, Authentication auth) {
        return ApiResponse.success(chat.sendImage(id, image, caption, auth.getName()));
    }
    @GetMapping("/conversations/{id}/messages/{messageId}/image")
    public org.springframework.http.ResponseEntity<ApiResponse<ImageData>> image(@PathVariable UUID id,
            @PathVariable UUID messageId, Authentication auth) {
        return org.springframework.http.ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(ApiResponse.success(chat.image(id, messageId, auth.getName())));
    }
    @GetMapping("/conversations")
    public ApiResponse<List<Conversation>> list(Authentication auth) {
        return ApiResponse.success(chat.list(auth.getName()));
    }
    @PostMapping("/rooms/{roomId}")
    public ApiResponse<Conversation> start(@PathVariable UUID roomId, Authentication auth) {
        return ApiResponse.success(chat.start(roomId, auth.getName()));
    }
    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<Message>> history(@PathVariable UUID id, @RequestParam(defaultValue = "0") int page, Authentication auth) {
        return ApiResponse.success(chat.history(id, page, auth.getName()));
    }
    @PostMapping("/conversations/{id}/messages")
    public ApiResponse<Message> send(@PathVariable UUID id, @Valid @RequestBody SendRequest body, Authentication auth) {
        return ApiResponse.success(chat.send(id, body.content(), auth.getName()));
    }
}
