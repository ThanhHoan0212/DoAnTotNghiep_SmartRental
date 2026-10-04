package com.phongtro.backend.repository;

import com.phongtro.backend.entity.VnpayPayment;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface VnpayPaymentRepository extends JpaRepository<VnpayPayment, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from VnpayPayment p where p.reference = :reference")
    Optional<VnpayPayment> findLockedByReference(String reference);
    Optional<VnpayPayment> findFirstByContractIdAndStatusOrderByCreatedAtDesc(UUID contractId, String status);
}
