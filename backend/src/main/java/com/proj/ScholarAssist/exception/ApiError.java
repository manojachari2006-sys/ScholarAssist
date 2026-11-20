package com.proj.ScholarAssist.exception;
import java.time.LocalDateTime;
import java.util.Map;

/** Stable error response shape shared by the REST exception handlers. */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fields) {
}
