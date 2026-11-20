package com.proj.ScholarAssist.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Converts expected application and validation failures into consistent API errors. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> unauthenticated(AuthenticationException exception) {
        return response(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password",
                Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden(AccessDeniedException exception) {
        return response(HttpStatus.FORBIDDEN, "You are not allowed to access this resource", Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler({DuplicateResourceException.class, DataIntegrityViolationException.class})
    ResponseEntity<ApiError> duplicate(Exception exception) {
        String message = exception instanceof DuplicateResourceException
                ? exception.getMessage()
                : "A record conflicts with existing data";
        return response(HttpStatus.CONFLICT, message, Map.of());
    }

    @ExceptionHandler(InvalidDocumentStateException.class)
    ResponseEntity<ApiError> invalidDocumentState(InvalidDocumentStateException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler({InvalidRuleException.class, IllegalArgumentException.class})
    ResponseEntity<ApiError> badRequest(RuntimeException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "Request validation failed", fields);
    }

    @ExceptionHandler({FileStorageException.class, MaxUploadSizeExceededException.class, MultipartException.class})
    ResponseEntity<ApiError> upload(Exception exception) {
        String message = exception instanceof FileStorageException
                ? exception.getMessage()
                : "File upload failed or exceeded the configured size limit";
        return response(HttpStatus.BAD_REQUEST, message, Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception) {
        // Keep internal exception details and stack traces out of client responses.
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred",
                Map.of());
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status,
            String message,
            Map<String, String> fields) {
        var error = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                fields);
        return ResponseEntity.status(status).body(error);
    }
}
