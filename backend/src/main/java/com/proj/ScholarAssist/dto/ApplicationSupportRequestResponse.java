package com.proj.ScholarAssist.dto;

import java.time.LocalDateTime;

public record ApplicationSupportRequestResponse(
        Long id,
        Long applicationId,
        Long studentId,
        String studentName,
        String studentEmail,
        String scholarshipName,
        String requestType,
        String studentMessage,
        String status,
        String reviewerMessage,
        LocalDateTime requestedAt,
        LocalDateTime reviewedAt) {
}
