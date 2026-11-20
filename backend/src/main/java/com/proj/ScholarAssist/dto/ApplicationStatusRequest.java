package com.proj.ScholarAssist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request for a controlled application review-status transition. */
public record ApplicationStatusRequest(
        @NotBlank String status,
        @NotBlank @Size(max = 2000) String remarks) {
}
