package com.proj.ScholarAssist.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Scholarship fields returned to clients without rule or entity relationships. */
@Getter
@Setter
@NoArgsConstructor
public class ScholarshipResponse {
    private Long id;
    private String name;
    private String provider;
    private String description;
    private String applicationUrl;
    private BigDecimal amount;
    private LocalDate deadline;
    private String educationLevel;
    private String state;
    private String category;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
