package com.proj.ScholarAssist.dto;

import java.util.List;

/** Preliminary result based only on answers supplied for this check. */
public record QuickEligibilityResponse(
        Long scholarshipId,
        String scholarshipName,
        String status,
        List<String> reasons,
        String notice) {
}
