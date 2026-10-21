package com.phongtro.backend.dto;


import com.phongtro.backend.entity.SupportComplaint;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class AdminDtos {
    private AdminDtos() {}
    public record CreateComplaint(
            @NotBlank @Size(max = 200) String subject,
            @NotNull SupportComplaint.Category category,
            @NotBlank @Size(min = 10, max = 5000) String description,
            @NotNull UUID contractId) {}
    public record HandleComplaint(
            @NotNull SupportComplaint.Status status,
            @NotBlank @Size(max = 5000) String response,
            @PositiveOrZero long version) {}
    public record ComplaintView(UUID id, UUID reporterId, String reporterName, String reporterEmail,
            UUID roomId, String roomTitle, UUID contractId, String contractCode, String subject, SupportComplaint.Category category,
            String description, SupportComplaint.Status status, String response, String handledBy,
            long version, Instant createdAt, Instant updatedAt) {}

}
