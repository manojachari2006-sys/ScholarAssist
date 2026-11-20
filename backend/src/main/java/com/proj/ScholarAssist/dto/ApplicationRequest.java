package com.proj.ScholarAssist.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

/** Client-supplied fields used to create a draft application. */
@Getter
@Setter
@NoArgsConstructor
public class ApplicationRequest {
    @NotNull
    private Long scholarshipId;

    @NotNull
    private LocalDate applicationDate;

    private String remarks;
}
