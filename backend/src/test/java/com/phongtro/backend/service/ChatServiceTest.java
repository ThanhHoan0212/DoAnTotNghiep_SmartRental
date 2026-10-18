package com.phongtro.backend.service;

import com.phongtro.backend.entity.*;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    @Mock ChatConversationRepository conversations;
    @Mock ChatMessageRepository messages;
    @Mock RoomRepository rooms;
    @Mock UserRepository users;
    @Mock ChatImageRepository images;
    @InjectMocks ChatService service;
    User tenant, landlord;
    Room room;
    ChatConversation conversation;

    @BeforeEach void setup() {
        tenant = User.builder().id(UUID.randomUUID()).email("tenant@test.vn").fullName("Khách thuê").role(Role.TENANT).build();
        landlord = User.builder().id(UUID.randomUUID()).email("owner@test.vn").fullName("Chủ nhà").role(Role.LANDLORD).build();
        room = Room.builder().id(UUID.randomUUID()).landlord(landlord).title("Phòng trọ").status(RoomStatus.APPROVED).build();
        conversation = new ChatConversation();
        conversation.setId(UUID.randomUUID()); conversation.setTenant(tenant); conversation.setLandlord(landlord); conversation.setRoom(room);
    }
    private void loggedIn(User user) { when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user)); }
    private void existing() { when(conversations.findById(conversation.getId())).thenReturn(Optional.of(conversation)); }

    private ChatMessage editable(User sender) {
        ChatMessage m = new ChatMessage(); m.setId(UUID.randomUUID()); m.setSender(sender);
        m.setConversation(conversation); m.setContent("Nội dung ban đầu"); m.setSentAt(Instant.now());
        when(messages.findByIdForUpdate(m.getId())).thenReturn(Optional.of(m));
        return m;
    }

    @Test void senderCanRecallAndAttachmentAndReactionsAreRemoved() {
        loggedIn(tenant); existing(); ChatMessage m = editable(tenant);
        m.setHasImage(true); m.getReactions().put(landlord.getId(), "❤️");
        var result = service.recall(conversation.getId(), m.getId(), tenant.getEmail());
        assertTrue(result.recalled()); assertEquals("Tin nhắn đã được thu hồi", result.content());
        assertFalse(result.hasImage()); assertTrue(result.reactions().isEmpty());
        verify(images).deleteById(m.getId()); verify(messages).saveAndFlush(m);
        service.recall(conversation.getId(), m.getId(), tenant.getEmail());
        verify(messages, times(1)).saveAndFlush(m);
    }

    @Test void recipientCannotRecallSomeoneElsesMessage() {
        loggedIn(landlord); existing(); ChatMessage m = editable(tenant);
        assertEquals(ErrorCode.UNAUTHORIZED, assertThrows(AppException.class,
                () -> service.recall(conversation.getId(), m.getId(), landlord.getEmail())).getErrorCode());
        verify(messages, never()).saveAndFlush(any()); verifyNoInteractions(images);
    }

    @Test void reactionsCanBeAddedChangedAndToggledWithoutOverwritingOthers() {
        loggedIn(tenant); existing(); ChatMessage m = editable(landlord);
        m.getReactions().put(landlord.getId(), "👍");
        service.react(conversation.getId(), m.getId(), "❤️", tenant.getEmail());
        assertEquals("❤️", m.getReactions().get(tenant.getId()));
        service.react(conversation.getId(), m.getId(), "😂", tenant.getEmail());
        assertEquals("😂", m.getReactions().get(tenant.getId()));
        service.react(conversation.getId(), m.getId(), "😂", tenant.getEmail());
        assertFalse(m.getReactions().containsKey(tenant.getId())); assertEquals("👍", m.getReactions().get(landlord.getId()));
    }

    @Test void recalledMessagesRejectReactionsAndAttachmentReads() {
        loggedIn(tenant); existing(); ChatMessage m = editable(tenant); m.setRecalled(true);
        assertThrows(AppException.class, () -> service.react(conversation.getId(), m.getId(), "👍", tenant.getEmail()));
        when(messages.findById(m.getId())).thenReturn(Optional.of(m));
        assertThrows(AppException.class, () -> service.image(conversation.getId(), m.getId(), tenant.getEmail()));
        verifyNoInteractions(images);
    }

    @Test void outsidersCannotRecallOrReact() {
        User outsider = User.builder().id(UUID.randomUUID()).email("outsider@test.vn").build(); loggedIn(outsider); existing();
        UUID id = UUID.randomUUID();
        assertThrows(AppException.class, () -> service.recall(conversation.getId(), id, outsider.getEmail()));
        assertThrows(AppException.class, () -> service.react(conversation.getId(), id, "👍", outsider.getEmail()));
        verifyNoInteractions(messages, images);
    }

    @Test void unsupportedReactionIsRejected() {
        loggedIn(tenant);
        assertThrows(AppException.class, () -> service.react(conversation.getId(), UUID.randomUUID(), "invalid", tenant.getEmail()));
        verifyNoInteractions(messages);
    }

    @Test void videoContainerIsStoredWithoutChangingBytes() {
        loggedIn(tenant); existing();
        var buffer = java.nio.ByteBuffer.allocate(42);
        buffer.putInt(24).put("ftypisom".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(0)
                .put("isommp42".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        buffer.putInt(9).put("moov".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).put((byte) 0);
        buffer.putInt(9).put("mdat".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).put((byte) 0);
        when(messages.saveAndFlush(any())).thenAnswer(call -> { ChatMessage m = call.getArgument(0); m.setId(UUID.randomUUID()); return m; });
        var result = service.sendVideo(conversation.getId(), new org.springframework.mock.web.MockMultipartFile("video", "test.mp4", "video/mp4", buffer.array()), "Video phòng", tenant.getEmail());
        assertTrue(result.hasVideo()); assertFalse(result.hasImage()); assertEquals("Video phòng", result.content());
        var captor = ArgumentCaptor.forClass(ChatImage.class); verify(images).save(captor.capture());
        assertEquals("video/mp4", captor.getValue().getContentType()); assertArrayEquals(buffer.array(), captor.getValue().getData());
    }

    @Test void fakeAndOversizedVideosCannotBeSent() {
        loggedIn(tenant); existing();
        var fake = new org.springframework.mock.web.MockMultipartFile("video", "fake.mp4", "video/mp4", "not really a video".getBytes());
        assertThrows(AppException.class, () -> service.sendVideo(conversation.getId(), fake, "", tenant.getEmail()));
        var large = mock(org.springframework.web.multipart.MultipartFile.class);
        when(large.getSize()).thenReturn(25L * 1024 * 1024 + 1);
        assertThrows(AppException.class, () -> service.sendVideo(conversation.getId(), large, "", tenant.getEmail()));
        verifyNoInteractions(messages, images);
    }

    @Test void outsiderCannotUploadReadOrListVideos() {
        User outsider = User.builder().id(UUID.randomUUID()).email("other@test.vn").build();
        loggedIn(outsider); existing();
        assertThrows(AppException.class, () -> service.sendVideo(conversation.getId(), null, "", outsider.getEmail()));
        assertThrows(AppException.class, () -> service.video(conversation.getId(), UUID.randomUUID(), outsider.getEmail()));
        assertThrows(AppException.class, () -> service.sharedMedia(conversation.getId(), 0, outsider.getEmail()));
        verifyNoInteractions(messages, images);
    }

    @Test void sharedMediaIncludesImagesAndVideos() {
        loggedIn(tenant); existing();
        ChatMessage image = new ChatMessage(); image.setSender(tenant); image.setHasImage(true);
        ChatMessage video = new ChatMessage(); video.setSender(landlord); video.setHasVideo(true);
        when(messages.findSharedMedia(conversation.getId(), org.springframework.data.domain.PageRequest.of(0, 20))).thenReturn(List.of(video, image));
        var result = service.sharedMedia(conversation.getId(), 0, tenant.getEmail());
        assertTrue(result.items().get(0).hasVideo()); assertTrue(result.items().get(1).hasImage()); assertFalse(result.hasMore());
    }

    @Test void sharedImagesUsesSeparatePaginatedHistory() {
        loggedIn(tenant); existing();
        List<ChatMessage> imageMessages = java.util.stream.IntStream.range(0, 20).mapToObj(i -> {
            ChatMessage m = new ChatMessage(); m.setId(UUID.randomUUID()); m.setSender(landlord);
            m.setContent("📷 Ảnh"); m.setHasImage(true); m.setSentAt(Instant.now()); return m;
        }).toList();
        when(messages.findByConversationIdAndHasImageTrueOrderBySentAtDescIdDesc(eq(conversation.getId()), any(Pageable.class)))
                .thenReturn(imageMessages);
        var result = service.sharedImages(conversation.getId(), 3, tenant.getEmail());
        assertEquals(20, result.items().size()); assertTrue(result.hasMore());
        assertTrue(result.items().stream().allMatch(m -> m.hasImage()));
        verify(messages).findByConversationIdAndHasImageTrueOrderBySentAtDescIdDesc(conversation.getId(), org.springframework.data.domain.PageRequest.of(3, 20));
        verifyNoInteractions(images);
    }

    @Test void outsiderCannotListSharedImages() {
        User outsider = User.builder().id(UUID.randomUUID()).email("other@test.vn").build();
        loggedIn(outsider); existing();
        assertThrows(AppException.class, () -> service.sharedImages(conversation.getId(), 0, outsider.getEmail()));
        verifyNoInteractions(messages, images);
    }

    @Test void sharedImagesRejectsNegativePageAndReturnsEmptyHistory() {
        loggedIn(tenant); existing();
        assertThrows(AppException.class, () -> service.sharedImages(conversation.getId(), -1, tenant.getEmail()));
        when(messages.findByConversationIdAndHasImageTrueOrderBySentAtDescIdDesc(eq(conversation.getId()), any(Pageable.class))).thenReturn(List.of());
        var result = service.sharedImages(conversation.getId(), 0, tenant.getEmail());
        assertTrue(result.items().isEmpty()); assertFalse(result.hasMore());
    }

    @Test void imageIsSavedWithCaptionAndCanBeReadByParticipant() throws Exception {
        loggedIn(tenant); existing();
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", output);
        byte[] data = output.toByteArray();
        when(messages.saveAndFlush(any())).thenAnswer(call -> {
            ChatMessage m = call.getArgument(0); m.setId(UUID.randomUUID()); return m;
        });
        var result = service.sendImage(conversation.getId(), new org.springframework.mock.web.MockMultipartFile("image", "test.png", "image/png", data), "  Ảnh phòng  ", tenant.getEmail());
        assertTrue(result.hasImage()); assertEquals("Ảnh phòng", result.content());
        var captor = ArgumentCaptor.forClass(ChatImage.class); verify(images).save(captor.capture());
        assertArrayEquals(data, captor.getValue().getData()); assertEquals("image/png", captor.getValue().getContentType());
        ChatMessage saved = new ChatMessage(); saved.setConversation(conversation);
        when(messages.findById(result.id())).thenReturn(Optional.of(saved));
        when(images.findById(result.id())).thenReturn(Optional.of(captor.getValue()));
        assertArrayEquals(data, service.image(conversation.getId(), result.id(), tenant.getEmail()).data());
    }

    @Test void fakeImageAndOversizedImageAreRejected() {
        loggedIn(tenant); existing();
        var fake = new org.springframework.mock.web.MockMultipartFile("image", "fake.png", "image/png", "not an image".getBytes());
        assertThrows(AppException.class, () -> service.sendImage(conversation.getId(), fake, "", tenant.getEmail()));
        var large = mock(org.springframework.web.multipart.MultipartFile.class);
        when(large.getSize()).thenReturn(5L * 1024 * 1024 + 1);
        assertThrows(AppException.class, () -> service.sendImage(conversation.getId(), large, "", tenant.getEmail()));
        verifyNoInteractions(images, messages);
    }

    @Test void outsiderCannotUploadOrReadImages() {
        User outsider = User.builder().id(UUID.randomUUID()).email("other@test.vn").build();
        loggedIn(outsider); existing();
        assertThrows(AppException.class, () -> service.sendImage(conversation.getId(), null, "", outsider.getEmail()));
        assertThrows(AppException.class, () -> service.image(conversation.getId(), UUID.randomUUID(), outsider.getEmail()));
        verifyNoInteractions(images, messages);
    }

    @Test void imageFromAnotherConversationCannotBeRead() {
        loggedIn(tenant); existing();
        ChatConversation other = new ChatConversation(); other.setId(UUID.randomUUID());
        ChatMessage m = new ChatMessage(); m.setConversation(other);
        UUID messageId = UUID.randomUUID(); when(messages.findById(messageId)).thenReturn(Optional.of(m));
        assertThrows(AppException.class, () -> service.image(conversation.getId(), messageId, tenant.getEmail()));
        verifyNoInteractions(images);
    }

    @Test void outsiderCannotReadOrSend() {
        User outsider = User.builder().id(UUID.randomUUID()).email("other@test.vn").build();
        loggedIn(outsider); existing();
        assertEquals(ErrorCode.UNAUTHORIZED, assertThrows(AppException.class,
                () -> service.history(conversation.getId(), 0, outsider.getEmail())).getErrorCode());
        assertThrows(AppException.class, () -> service.send(conversation.getId(), "hello", outsider.getEmail()));
        verifyNoInteractions(messages);
    }
    @Test void conversationIncludesBasicProfileOfOtherParticipant() {
        loggedIn(tenant);
        landlord.setAvatarUrl("/uploads/avatar.jpg");
        landlord.setIdentityVerified(true);
        landlord.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        when(conversations.findForUser(tenant.getId())).thenReturn(List.of(conversation));
        var result = service.list(tenant.getEmail()).get(0);
        assertEquals(landlord.getId(), result.otherUserId());
        assertEquals(landlord.getFullName(), result.otherUserName());
        assertEquals(landlord.getEmail(), result.otherUserEmail());
        assertEquals(landlord.getAvatarUrl(), result.otherUserAvatarUrl());
        assertEquals("LANDLORD", result.otherUserRole());
        assertTrue(result.otherUserIdentityVerified());
        assertEquals(landlord.getCreatedAt(), result.otherUserCreatedAt());
    }
    @Test void tenantStartsAndReusesRoomConversation() {
        loggedIn(tenant);
        when(rooms.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(conversations.findByRoomIdAndTenantId(room.getId(), tenant.getId())).thenReturn(Optional.empty(), Optional.of(conversation));
        when(conversations.saveAndFlush(any())).thenAnswer(call -> {
            ChatConversation c = call.getArgument(0); c.setId(conversation.getId()); return c;
        });
        assertEquals(landlord.getId(), service.start(room.getId(), tenant.getEmail()).otherUserId());
        assertEquals(conversation.getId(), service.start(room.getId(), tenant.getEmail()).id());
        verify(conversations, times(1)).saveAndFlush(any());
    }
    @Test void cannotChatWithOwnRoom() {
        loggedIn(landlord);
        when(rooms.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        assertThrows(AppException.class, () -> service.start(room.getId(), landlord.getEmail()));
        verify(conversations, never()).saveAndFlush(any());
    }

    @Test void outsiderCannotReadOrSendInDirectConversation() {
        conversation.setRoom(null);
        outsiderCannotReadOrSend();
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(Role.class)
    void anyRoleCanContactRoomOwner(Role role) {
        tenant.setRole(role);
        tenantStartsAndReusesRoomConversation();
    }
    @Test void hiddenRoomCannotStartConversation() {
        loggedIn(tenant); room.setStatus(RoomStatus.HIDDEN);
        when(rooms.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(conversations.findByRoomIdAndTenantId(room.getId(), tenant.getId())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.start(room.getId(), tenant.getEmail()));
        verify(conversations, never()).saveAndFlush(any());
    }
    @Test void bothParticipantsCanSendAndContentIsTrimmed() {
        existing();
        when(messages.save(any())).thenAnswer(call -> { ChatMessage m = call.getArgument(0); m.setId(UUID.randomUUID()); return m; });
        for (User sender : List.of(tenant, landlord)) {
            loggedIn(sender);
            var result = service.send(conversation.getId(), "  Xin chào  ", sender.getEmail());
            assertEquals(sender.getId(), result.senderId()); assertEquals("Xin chào", result.content());
            assertNotNull(result.sentAt());
        }
    }
    @Test void invalidContentIsNeverSaved() {
        loggedIn(tenant); existing();
        for (String content : Arrays.asList(null, "  \n "))
            assertThrows(AppException.class, () -> service.send(conversation.getId(), content, tenant.getEmail()));
        verifyNoInteractions(messages);
    }
    @Test void longMessagesAreSavedWithoutTruncation() {
        loggedIn(tenant); existing();
        when(messages.save(any())).thenAnswer(call -> call.getArgument(0));
        String content = "Tin nhắn dài. ".repeat(2000).strip();
        assertEquals(content, service.send(conversation.getId(), content, tenant.getEmail()).content());
    }
    @Test void blockedRecipientCannotReceiveMessages() {
        loggedIn(tenant); existing(); landlord.setStatus(UserStatus.BANNED);
        assertThrows(AppException.class, () -> service.send(conversation.getId(), "hello", tenant.getEmail()));
        verifyNoInteractions(messages);
    }
    @Test void historyReturnsChronologicalPage() {
        loggedIn(landlord); existing();
        ChatMessage old = new ChatMessage(); old.setSender(tenant); old.setContent("old"); old.setSentAt(Instant.parse("2026-10-01T00:00:00Z"));
        ChatMessage recent = new ChatMessage(); recent.setSender(landlord); recent.setContent("new"); recent.setSentAt(Instant.parse("2026-10-02T00:00:00Z"));
        when(messages.findByConversationIdOrderBySentAtDescIdDesc(eq(conversation.getId()), any(Pageable.class))).thenReturn(List.of(recent, old));
        var result = service.history(conversation.getId(), 0, landlord.getEmail());
        assertEquals(List.of("old", "new"), result.stream().map(m -> m.content()).toList());
        assertThrows(AppException.class, () -> service.history(conversation.getId(), -1, landlord.getEmail()));
    }
}
