package com.proj.ScholarAssist.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/** Stored eligibility decision for a student and scholarship pair. */
@Getter
@Setter
@NoArgsConstructor
public class EligibilityResponse {
    private Long studentId;
    private Long scholarshipId;
    private String status;
    private String reason;
    private LocalDateTime evaluatedAt;
}
