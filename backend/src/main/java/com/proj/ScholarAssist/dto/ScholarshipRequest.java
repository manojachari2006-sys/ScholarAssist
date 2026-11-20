package com.proj.ScholarAssist.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Client-supplied scholarship data and optional eligibility rules. */
@Getter
@Setter
@NoArgsConstructor
public class ScholarshipRequest {
    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Size(max = 200)
    private String provider;

    private String description;

    @Size(max = 1000)
    @Pattern(regexp = "^https://.*$", message = "Application URL must use HTTPS")
    private String applicationUrl;

    @DecimalMin("0.0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;

    private LocalDate deadline;

    @Size(max = 100)
    private String educationLevel;

    @Size(max = 100)
    private String state;

    @Size(max = 50)
    private String category;

    @NotBlank
    @Size(max = 30)
    private String status;

    private List<@Valid ScholarshipRuleRequest> rules;
}
