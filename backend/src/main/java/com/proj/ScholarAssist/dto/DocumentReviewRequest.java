package com.proj.ScholarAssist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** A reviewer decision and, when rejected, feedback for the student. */
public record DocumentReviewRequest(
        @NotBlank
        @Pattern(regexp = "VERIFIED|REJECTED", message = "Status must be VERIFIED or REJECTED")
        String status,
        @Size(max = 2000)
        String feedback) {
}
