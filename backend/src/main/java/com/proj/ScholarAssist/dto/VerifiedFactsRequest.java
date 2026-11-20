package com.proj.ScholarAssist.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Reviewer-confirmed facts derived from one reviewed document. */
public record VerifiedFactsRequest(@NotEmpty List<@Valid VerifiedFactRequest> facts) {
}
