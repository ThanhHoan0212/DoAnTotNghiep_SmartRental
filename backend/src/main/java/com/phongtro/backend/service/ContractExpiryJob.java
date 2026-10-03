package com.phongtro.backend.service;

import com.phongtro.backend.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ContractExpiryJob {
    private final ContractRepository contractRepository;
    private final ContractService contractService;

    // Each agreement runs in its own transaction; a concurrent change is retried next run.
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void expireContracts() {
        for (UUID id : contractRepository.findExpiredActiveIds(LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")), java.time.Instant.now(), PageRequest.of(0, 100))) {
            try {
                contractService.expireContract(id);
            } catch (RuntimeException ex) {
                log.warn("Could not expire contract [{}]; will retry", id, ex);
            }
        }
    }
}
