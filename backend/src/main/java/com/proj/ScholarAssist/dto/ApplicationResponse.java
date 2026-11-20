package com.proj.ScholarAssist.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Response view of an application and its scholarship summary. */
@Getter
@Setter
@NoArgsConstructor
public class ApplicationResponse {
    private Long id;
    private Long scholarshipId;
    private String scholarshipName;
    private LocalDate scholarshipDeadline;
    private LocalDate applicationDate;
    private String status;
    private String remarks;
    private String providerApplicationId;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String latestUpdate;
    private String latestUpdateSource;
    private LocalDateTime latestUpdateAt;
}
