package com.proj.ScholarAssist.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

/** Student-entered answers for a preliminary scholarship criteria check. */
public record QuickEligibilityRequest(
        @NotNull(message = "Facts map is required")
        Map<String, String> facts,
        Map<Long, Boolean> additionalConditions) {
}
