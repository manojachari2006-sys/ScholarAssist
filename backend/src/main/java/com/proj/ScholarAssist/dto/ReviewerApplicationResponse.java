package com.proj.ScholarAssist.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Application fields reviewers need to decide and communicate an outcome. */
public record ReviewerApplicationResponse(
        Long id,
        Long studentId,
        String studentName,
        String studentEmail,
        String scholarshipName,
        String scholarshipProvider,
        String status,
        String providerApplicationId,
        LocalDate applicationDate,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt,
        String remarks) {
}
