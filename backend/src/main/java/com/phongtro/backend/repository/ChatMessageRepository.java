package com.phongtro.backend.repository;

import com.phongtro.backend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select m from ChatMessage m where m.id = :id")
    Optional<ChatMessage> findByIdForUpdate(UUID id);
    @org.springframework.data.jpa.repository.Query("select m from ChatMessage m where m.conversation.id = :id and (m.hasImage = true or m.hasVideo = true) order by m.sentAt desc, m.id desc")
    List<ChatMessage> findSharedMedia(UUID id, Pageable pageable);
    List<ChatMessage> findByConversationIdOrderBySentAtDescIdDesc(UUID conversationId, Pageable pageable);
    List<ChatMessage> findByConversationIdAndHasImageTrueOrderBySentAtDescIdDesc(UUID conversationId, Pageable pageable);
}
