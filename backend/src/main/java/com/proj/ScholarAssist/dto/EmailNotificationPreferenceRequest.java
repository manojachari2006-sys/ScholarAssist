package com.proj.ScholarAssist.dto;

import jakarta.validation.constraints.NotNull;

/** Student choice to receive transactional notification emails. */
public record EmailNotificationPreferenceRequest(@NotNull Boolean enabled) {}
