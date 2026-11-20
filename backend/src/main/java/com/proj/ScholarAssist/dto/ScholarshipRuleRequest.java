package com.proj.ScholarAssist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Client-supplied eligibility rule attached to a scholarship request. */
public record ScholarshipRuleRequest(
        @NotBlank
        @Size(max = 50)
        String ruleType,
        @NotBlank
        @Size(max = 10)
        String operator,
        @NotBlank
        @Size(max = 255)
        String ruleValue,
        @Size(max = 500)
        String description) {
}
