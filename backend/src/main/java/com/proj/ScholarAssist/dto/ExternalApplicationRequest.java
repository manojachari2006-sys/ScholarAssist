package com.proj.ScholarAssist.dto;

import jakarta.validation.constraints.NotNull;

/** Identifies the scholarship whose external application the student is starting. */
public record ExternalApplicationRequest(@NotNull Long scholarshipId) {
}
