package com.phongtro.backend.service;

import com.phongtro.backend.exception.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminAnalyticsServiceTest {
    @Test void invalidOrUnboundedDateRangeRejectedBeforeDatabaseQuery() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        AdminAnalyticsService service = new AdminAnalyticsService(jdbc);
        assertThrows(AppException.class, () -> service.report(LocalDate.of(2026,10,18), LocalDate.of(2026,10,1)));
        assertThrows(AppException.class, () -> service.report(LocalDate.of(2020,1,1), LocalDate.of(2026,10,18)));
        verifyNoInteractions(jdbc);
    }
}
