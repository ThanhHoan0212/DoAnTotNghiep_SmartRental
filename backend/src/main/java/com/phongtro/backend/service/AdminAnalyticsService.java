package com.phongtro.backend.service;

import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor
public class AdminAnalyticsService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final JdbcTemplate jdbc;
    public record Report(LocalDate from, LocalDate to, Map<String, Object> snapshot,
            List<Map<String, Object>> monthly, List<Map<String, Object>> roomStatuses,
            List<Map<String, Object>> userRoles, List<Map<String, Object>> districts,
            List<Map<String, Object>> contractStatuses) {}

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Report report(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now(ZONE) : to;
        LocalDate start = from == null ? end.withDayOfMonth(1).minusMonths(5) : from;
        if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) > 1096) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Khoảng báo cáo phải hợp lệ và không vượt quá 3 năm");
        }
        Timestamp since = Timestamp.from(start.atStartOfDay(ZONE).toInstant());
        Timestamp until = Timestamp.from(end.plusDays(1).atStartOfDay(ZONE).toInstant());
        Map<String, Object> snapshot = jdbc.queryForMap("""
            SELECT (SELECT COUNT(*) FROM users) AS "totalUsers",
              (SELECT COUNT(*) FROM users WHERE status = 'ACTIVE') AS "activeUsers",
              (SELECT COUNT(*) FROM rooms) AS "totalRooms",
              (SELECT COUNT(*) FROM rooms WHERE status = 'PENDING') AS "pendingRooms",
              (SELECT COUNT(*) FROM rooms WHERE status = 'RENTED') AS "rentedRooms",
              (SELECT COUNT(*) FROM contracts WHERE status = 'ACTIVE') AS "activeContracts",
              (SELECT COUNT(*) FROM support_complaints WHERE status IN ('OPEN','IN_PROGRESS')) AS "openComplaints",
              (SELECT COALESCE(AVG(price),0) FROM rooms WHERE status IN ('APPROVED','RESERVED','RENTED')) AS "averageRent"
            """);
        // Aggregate each source independently to avoid multiplying counts through joins.
        List<Map<String, Object>> monthly = jdbc.queryForList("""
            WITH months AS (
              SELECT generate_series(date_trunc('month', ?::timestamptz AT TIME ZONE 'Asia/Ho_Chi_Minh'),
                date_trunc('month', (?::timestamptz - interval '1 microsecond') AT TIME ZONE 'Asia/Ho_Chi_Minh'), interval '1 month') AS month
            ), u AS (
              SELECT date_trunc('month', created_at AT TIME ZONE 'Asia/Ho_Chi_Minh') AS month, COUNT(*) AS count
              FROM users WHERE created_at >= ? AND created_at < ? GROUP BY 1
            ), r AS (
              SELECT date_trunc('month', created_at AT TIME ZONE 'Asia/Ho_Chi_Minh') AS month, COUNT(*) AS count
              FROM rooms WHERE created_at >= ? AND created_at < ? GROUP BY 1
            ), c AS (
              SELECT date_trunc('month', created_at AT TIME ZONE 'Asia/Ho_Chi_Minh') AS month, COUNT(*) AS count
              FROM contracts WHERE created_at >= ? AND created_at < ? GROUP BY 1
            ), p AS (
              SELECT date_trunc('month', completed_at AT TIME ZONE 'Asia/Ho_Chi_Minh') AS month, SUM(amount) AS amount
              FROM vnpay_payments WHERE status = 'SUCCESS' AND completed_at >= ? AND completed_at < ? GROUP BY 1
            )
            SELECT to_char(m.month, 'YYYY-MM') AS month, COALESCE(u.count,0) AS users,
              COALESCE(r.count,0) AS rooms, COALESCE(c.count,0) AS contracts, COALESCE(p.amount,0) AS "depositCollected"
            FROM months m LEFT JOIN u USING(month) LEFT JOIN r USING(month) LEFT JOIN c USING(month) LEFT JOIN p USING(month)
            ORDER BY m.month
            """, since, until, since, until, since, until, since, until, since, until);
        return new Report(start, end, snapshot, monthly,
                jdbc.queryForList("SELECT status AS label, COUNT(*) AS count FROM rooms GROUP BY status ORDER BY count DESC"),
                jdbc.queryForList("SELECT role AS label, COUNT(*) AS count FROM users GROUP BY role ORDER BY count DESC"),
                jdbc.queryForList("""
                  SELECT district AS label, COUNT(*) AS count, COALESCE(AVG(price),0) AS "averageRent"
                  FROM rooms WHERE status IN ('APPROVED','RESERVED','RENTED')
                  GROUP BY district ORDER BY count DESC, district LIMIT 10
                  """),
                jdbc.queryForList("SELECT status AS label, COUNT(*) AS count FROM contracts WHERE created_at >= ? AND created_at < ? GROUP BY status ORDER BY count DESC", since, until));
    }
}
