package com.proj.ScholarAssist.dto;

import java.time.LocalDateTime;

/** Auditable reviewer confirmation without exposing stored file paths. */
public record VerifiedFactResponse(
        Long id,
        Long studentId,
        String factType,
        String value,
        Long sourceDocumentId,
        String sourceFileName,
        Long reviewerId,
        LocalDateTime verifiedAt) {
}
