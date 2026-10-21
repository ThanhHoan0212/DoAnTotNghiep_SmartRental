package com.phongtro.backend.repository;

import com.phongtro.backend.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.*;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {
    Optional<ChatConversation> findByRoomIdAndTenantId(UUID roomId, UUID tenantId);
    @Query("select c from ChatConversation c left join fetch c.room join fetch c.tenant join fetch c.landlord where c.tenant.id = :userId or c.landlord.id = :userId order by c.updatedAt desc, c.id desc")
    List<ChatConversation> findForUser(UUID userId);
}
