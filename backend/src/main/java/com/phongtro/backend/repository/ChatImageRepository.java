package com.phongtro.backend.repository;

import com.phongtro.backend.entity.ChatImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ChatImageRepository extends JpaRepository<ChatImage, UUID> {}
