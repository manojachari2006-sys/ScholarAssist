package com.proj.ScholarAssist.dto;

import java.time.LocalDateTime;

/** Document review queue item without its internal local storage path. */
public record ReviewerDocumentResponse(
        Long id,
        Long studentId,
        String studentName,
        String documentType,
        String fileName,
        String verificationStatus,
        LocalDateTime uploadedAt,
        LocalDateTime reviewedAt,
        String reviewFeedback) {
}
