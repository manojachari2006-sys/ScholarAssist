package com.proj.ScholarAssist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicationSupportRequestDecision(
        @NotBlank String decision,
        @NotBlank @Size(max = 2000) String message) {
}
