package com.proj.ScholarAssist.dto;

import jakarta.validation.constraints.Size;

/** Optional confirmation details after the student submits on a provider website. */
public record ExternalApplicationSubmissionRequest(
        @Size(max = 150, message = "Provider application number cannot exceed 150 characters")
        String providerApplicationId,
        @Size(max = 2000, message = "Remarks cannot exceed 2000 characters")
        String remarks) {
}
