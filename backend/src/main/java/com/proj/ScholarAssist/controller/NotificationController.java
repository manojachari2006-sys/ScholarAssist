package com.proj.ScholarAssist.controller;

import com.proj.ScholarAssist.dto.NotificationResponse;
import com.proj.ScholarAssist.dto.EmailNotificationPreferenceRequest;
import com.proj.ScholarAssist.dto.EmailNotificationPreferenceResponse;
import com.proj.ScholarAssist.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.Authentication;
import java.util.List;

/** HTTP endpoints for listing notifications and marking them read. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping("/preferences/email")
    public ResponseEntity<EmailNotificationPreferenceResponse> emailPreference(
            Authentication authentication) {
        return ResponseEntity.ok(service.emailPreference(authentication.getName()));
    }

    @PutMapping("/preferences/email")
    public ResponseEntity<EmailNotificationPreferenceResponse> setEmailPreference(
            @Valid @RequestBody EmailNotificationPreferenceRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.setEmailPreference(
                request.enabled(), authentication.getName()));
    }

    /** Lists all notifications for a user, newest first. */
    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationResponse>> all(
            @PathVariable Long userId,
            Authentication authentication) {
        return ResponseEntity.ok(service.byUser(userId, false, authentication.getName()));
    }

    /** Lists unread notifications for a user, newest first. */
    @GetMapping("/{userId}/unread")
    public ResponseEntity<List<NotificationResponse>> unread(
            @PathVariable Long userId,
            Authentication authentication) {
        return ResponseEntity.ok(service.byUser(userId, true, authentication.getName()));
    }

    /** Marks one notification as read. */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> read(
            @PathVariable Long notificationId,
            Authentication authentication) {
        return ResponseEntity.ok(service.markRead(notificationId, authentication.getName()));
    }
}
