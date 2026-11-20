package com.proj.ScholarAssist.dto;

import java.time.LocalDateTime;

/** Safe, chronological view of one application status change. */
public record ApplicationStatusHistoryResponse(
        Long id,
        String previousStatus,
        String newStatus,
        String note,
        String changeSource,
        String changedByRole,
        LocalDateTime changedAt) {
}
