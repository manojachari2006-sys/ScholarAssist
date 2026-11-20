package com.proj.ScholarAssist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** One field a reviewer confirmed from the associated verified document. */
public record VerifiedFactRequest(
        @NotBlank String factType,
        @NotBlank @Size(max = 255) String value) {
}
