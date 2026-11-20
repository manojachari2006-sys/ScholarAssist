package com.proj.ScholarAssist.dto;

/** Email preference and whether server-side email delivery is configured. */
public record EmailNotificationPreferenceResponse(boolean enabled, boolean deliveryAvailable) {}
