package com.phongtro.backend.repository;

import com.phongtro.backend.entity.EkycVerification;
import com.phongtro.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EkycVerificationRepository extends JpaRepository<EkycVerification, UUID> {

    List<EkycVerification> findByUserOrderByCreatedAtDesc(User user);

    Optional<EkycVerification> findFirstByUserOrderByCreatedAtDesc(User user);

    boolean existsByIdCardNumberAndStatus(String idCardNumber, String status);
}
