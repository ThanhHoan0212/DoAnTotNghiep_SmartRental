package com.phongtro.backend.service;

import com.phongtro.backend.dto.ChatDtos.*;
import com.phongtro.backend.entity.*;
import com.phongtro.backend.exception.*;
import com.phongtro.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class ChatService {
    private final ChatConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final RoomRepository rooms;
    private final UserRepository users;
    private final ChatImageRepository images;

    private ChatMessage editableMessage(UUID id, UUID messageId, User user) {
        participant(id, user);
        ChatMessage m = messages.findByIdForUpdate(messageId).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!m.getConversation().getId().equals(id)) throw new AppException(ErrorCode.UNAUTHORIZED);
        return m;
    }

    @Transactional
    public Message recall(UUID id, UUID messageId, String email) {
        User sender = user(email);
        ChatMessage m = editableMessage(id, messageId, sender);
        if (!m.getSender().getId().equals(sender.getId())) throw new AppException(ErrorCode.UNAUTHORIZED);
        if (!m.isRecalled()) {
            if (m.isHasImage() || m.isHasVideo()) images.deleteById(messageId);
            m.setRecalled(true); m.setContent("Tin nhắn đã được thu hồi");
            m.setHasImage(false); m.setHasVideo(false); m.getReactions().clear();
            messages.saveAndFlush(m);
        }
        return messageView(m);
    }

    @Transactional
    public Message react(UUID id, UUID messageId, String emoji, String email) {
        User user = user(email);
        if (!Set.of("👍", "❤️", "😂", "😮", "😢", "😡").contains(emoji == null ? "" : emoji))
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        ChatMessage m = editableMessage(id, messageId, user);
        if (m.isRecalled()) throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "Không thể thả cảm xúc cho tin nhắn đã thu hồi.");
        if (emoji.equals(m.getReactions().get(user.getId()))) m.getReactions().remove(user.getId());
        else m.getReactions().put(user.getId(), emoji);
        messages.saveAndFlush(m);
        return messageView(m);
    }

    @Transactional
    public Message sendVideo(UUID id, org.springframework.web.multipart.MultipartFile file, String caption, String email) {
        User sender = user(email);
        ChatConversation c = participant(id, sender);
        if (c.getTenant().getStatus() != UserStatus.ACTIVE || c.getLandlord().getStatus() != UserStatus.ACTIVE)
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED);
        if (file == null || file.isEmpty() || file.getSize() > 25 * 1024 * 1024)
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Video phải có dung lượng từ 1 byte đến 25 MB.");
        byte[] bytes;
        try { bytes = file.getBytes(); }
        catch (java.io.IOException ex) { throw new AppException(ErrorCode.VALIDATION_FAILED, "Không đọc được video."); }
        // Check container structure rather than trusting the filename or submitted MIME type.
        boolean mp4 = false;
        if (bytes.length >= 24) {
            var buffer = java.nio.ByteBuffer.wrap(bytes);
            long size = Integer.toUnsignedLong(buffer.getInt());
            String type = new String(bytes, 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if (type.equals("ftyp") && size >= 16 && size <= bytes.length && size <= 4096) {
                var brands = Set.of("isom", "iso2", "iso3", "iso4", "iso5", "iso6", "mp41", "mp42", "avc1", "dash", "M4V ");
                for (int offset = 8; offset + 4 <= size; offset += 4) {
                    if (offset != 12 && brands.contains(new String(bytes, offset, 4, java.nio.charset.StandardCharsets.US_ASCII))) mp4 = true;
                }
                boolean media = false, metadata = false;
                long offset = size;
                while (mp4 && offset + 8 <= bytes.length) {
                    buffer.position((int) offset);
                    long boxSize = Integer.toUnsignedLong(buffer.getInt());
                    String boxType = new String(bytes, (int) offset + 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
                    if (boxSize == 1) { if (offset + 16 > bytes.length) { mp4 = false; break; } boxSize = buffer.getLong((int) offset + 8); }
                    if (boxSize == 0) boxSize = bytes.length - offset;
                    if (boxSize < 8 || boxSize > bytes.length - offset) { mp4 = false; break; }
                    media |= boxType.equals("mdat") && boxSize > 8;
                    metadata |= boxType.equals("moov") && boxSize > 8;
                    offset += boxSize;
                }
                mp4 &= media && metadata && offset == bytes.length;
            }
        }
        if (!mp4) throw new AppException(ErrorCode.VALIDATION_FAILED, "Chọn video MP4 hợp lệ (khuyến nghị H.264/AAC), tối đa 25 MB.");
        ChatMessage m = new ChatMessage(); m.setConversation(c); m.setSender(sender);
        m.setContent(caption == null || caption.isBlank() ? "🎬 Video" : caption.strip()); m.setSentAt(Instant.now()); m.setHasVideo(true);
        c.setUpdatedAt(m.getSentAt()); conversations.save(c); messages.saveAndFlush(m);
        ChatImage media = new ChatImage(); media.setMessageId(m.getId()); media.setContentType("video/mp4"); media.setData(bytes); images.save(media);
        return messageView(m);
    }

    public ImageData video(UUID id, UUID messageId, String email) {
        ImageData media = image(id, messageId, email);
        if (!media.contentType().equals("video/mp4")) throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        return media;
    }

    public ImagePage sharedMedia(UUID id, int page, String email) {
        participant(id, user(email));
        if (page < 0) throw new AppException(ErrorCode.VALIDATION_FAILED);
        List<ChatMessage> result = messages.findSharedMedia(id, PageRequest.of(page, 20));
        return new ImagePage(result.stream().map(this::messageView).toList(), result.size() == 20);
    }

    @Transactional
    public Message sendImage(UUID id, org.springframework.web.multipart.MultipartFile file, String caption, String email) {
        User sender = user(email);
        ChatConversation c = participant(id, sender);
        if (c.getTenant().getStatus() != UserStatus.ACTIVE || c.getLandlord().getStatus() != UserStatus.ACTIVE)
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED);
        if (file == null || file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Ảnh phải có dung lượng từ 1 byte đến 5 MB.");
        byte[] data;
        String contentType;
        try {
            data = file.getBytes();
            try (var input = javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(data))) {
                var readers = javax.imageio.ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new java.io.IOException("Invalid image");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    contentType = switch (format) {
                        case "png" -> "image/png";
                        case "jpeg", "jpg" -> "image/jpeg";
                        case "gif" -> "image/gif";
                        default -> throw new java.io.IOException("Unsupported image");
                    };
                    if (reader.getWidth(0) <= 0 || reader.getHeight(0) <= 0 ||
                            (long) reader.getWidth(0) * reader.getHeight(0) > 20_000_000)
                        throw new java.io.IOException("Image dimensions too large");
                    if (reader.read(0) == null) throw new java.io.IOException("Invalid image data");
                } finally { reader.dispose(); }
            }
        } catch (java.io.IOException | RuntimeException ex) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Ảnh không hợp lệ. Chọn JPG, PNG hoặc GIF, tối đa 20 triệu điểm ảnh.");
        }
        ChatMessage m = new ChatMessage();
        m.setConversation(c); m.setSender(sender); m.setContent(caption == null || caption.isBlank() ? "📷 Ảnh" : caption.strip());
        m.setSentAt(Instant.now()); m.setHasImage(true);
        c.setUpdatedAt(m.getSentAt()); conversations.save(c);
        messages.saveAndFlush(m);
        ChatImage image = new ChatImage();
        image.setMessageId(m.getId()); image.setContentType(contentType); image.setData(data);
        images.save(image);
        return messageView(m);
    }

    public ImageData image(UUID conversationId, UUID messageId, String email) {
        participant(conversationId, user(email));
        ChatMessage m = messages.findById(messageId).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!m.getConversation().getId().equals(conversationId)) throw new AppException(ErrorCode.UNAUTHORIZED);
        if (m.isRecalled()) throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        ChatImage image = images.findById(messageId).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        return new ImageData(image.getContentType(), image.getData());
    }

    public ImagePage sharedImages(UUID id, int page, String email) {
        participant(id, user(email));
        if (page < 0) throw new AppException(ErrorCode.VALIDATION_FAILED);
        List<ChatMessage> result = messages.findByConversationIdAndHasImageTrueOrderBySentAtDescIdDesc(id, PageRequest.of(page, 20));
        return new ImagePage(result.stream().map(this::messageView).toList(), result.size() == 20);
    }

    private User user(String email) {
        User user = users.findByEmail(email).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) throw new AppException(ErrorCode.UNAUTHORIZED);
        return user;
    }

    private ChatConversation participant(UUID id, User user) {
        ChatConversation c = conversations.findById(id).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!c.getTenant().getId().equals(user.getId()) && !c.getLandlord().getId().equals(user.getId()))
            throw new AppException(ErrorCode.UNAUTHORIZED);
        return c;
    }

    private Conversation view(ChatConversation c, User user) {
        User other = c.getTenant().getId().equals(user.getId()) ? c.getLandlord() : c.getTenant();
        return new Conversation(c.getId(), c.getRoom() == null ? null : c.getRoom().getId(),
                c.getRoom() == null ? null : c.getRoom().getTitle(), other.getId(), other.getFullName(), other.getEmail(), other.getAvatarUrl(),
                other.getRole().name(), other.isIdentityVerified(), other.getCreatedAt(), c.getUpdatedAt());
    }

    public List<Conversation> list(String email) {
        User user = user(email);
        return conversations.findForUser(user.getId()).stream().map(c -> view(c, user)).toList();
    }

    @Transactional
    public Conversation start(UUID roomId, String email) {
        User tenant = user(email);
        // Serialize creation for a room so concurrent requests reuse the same conversation.
        Room room = rooms.findByIdForUpdate(roomId).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        Optional<ChatConversation> existing = conversations.findByRoomIdAndTenantId(roomId, tenant.getId());
        if (existing.isPresent()) return view(existing.get(), tenant);
        if (room.getLandlord().getId().equals(tenant.getId()) || room.getLandlord().getStatus() != UserStatus.ACTIVE)
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED);
        if (!Set.of(RoomStatus.APPROVED, RoomStatus.RESERVED, RoomStatus.RENTED).contains(room.getStatus()))
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED);
        ChatConversation c = new ChatConversation();
        c.setRoom(room); c.setTenant(tenant); c.setLandlord(room.getLandlord());
        return view(conversations.saveAndFlush(c), tenant);
    }

    public List<Message> history(UUID id, int page, String email) {
        participant(id, user(email));
        if (page < 0) throw new AppException(ErrorCode.VALIDATION_FAILED);
        List<ChatMessage> result = new ArrayList<>(messages.findByConversationIdOrderBySentAtDescIdDesc(id, PageRequest.of(page, 50)));
        Collections.reverse(result);
        return result.stream().map(this::messageView).toList();
    }

    @Transactional
    public Message send(UUID id, String content, String email) {
        User sender = user(email);
        ChatConversation c = participant(id, sender);
        if (c.getTenant().getStatus() != UserStatus.ACTIVE || c.getLandlord().getStatus() != UserStatus.ACTIVE)
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED);
        if (content == null || content.isBlank())
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        ChatMessage m = new ChatMessage();
        m.setConversation(c); m.setSender(sender); m.setContent(content.strip()); m.setSentAt(Instant.now());
        c.setUpdatedAt(m.getSentAt());
        conversations.save(c);
        return messageView(messages.save(m));
    }

    private Message messageView(ChatMessage m) {
        return new Message(m.getId(), m.getSender().getId(), m.isRecalled() ? "Tin nhắn đã được thu hồi" : m.getContent(),
                m.getSentAt(), !m.isRecalled() && m.isHasImage(), !m.isRecalled() && m.isHasVideo(), m.isRecalled(), m.getRevision(),
                m.isRecalled() ? List.of() : m.getReactions().entrySet().stream().map(e -> new Reaction(e.getKey(), e.getValue())).toList());
    }
}
