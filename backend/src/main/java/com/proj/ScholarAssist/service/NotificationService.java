package com.proj.ScholarAssist.service;

import com.proj.ScholarAssist.dto.NotificationResponse;
import com.proj.ScholarAssist.entity.Notification;
import com.proj.ScholarAssist.entity.NotificationEmailOutbox;
import com.proj.ScholarAssist.entity.User;
import com.proj.ScholarAssist.dto.EmailNotificationPreferenceResponse;
import com.proj.ScholarAssist.exception.ResourceNotFoundException;
import com.proj.ScholarAssist.repository.NotificationEmailOutboxRepository;
import com.proj.ScholarAssist.repository.NoticationRepository;
import com.proj.ScholarAssist.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;

/** Creates notifications and supports user notification queries and read updates. */
@Service
public class NotificationService {
    private final NoticationRepository notifications;
    private final UserRepository users;
    private final NotificationEmailOutboxRepository emailOutbox;

    @org.springframework.beans.factory.annotation.Value("${scholarassist.email.enabled:false}")
    private boolean emailDeliveryAvailable;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.host:}")
    private String mailHost;

    public NotificationService(
            NoticationRepository notifications,
            UserRepository users,
            NotificationEmailOutboxRepository emailOutbox) {
        this.notifications = notifications;
        this.users = users;
        this.emailOutbox = emailOutbox;
    }

    /** Lists all or unread notifications for an existing user, newest first. */
    @Transactional(readOnly = true)
    public List<NotificationResponse> byUser(Long userId, boolean unreadOnly, String email) {
        var user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        requireOwner(user.getEmail(), email);

        var userNotifications = unreadOnly
                ? notifications.findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(userId)
                : notifications.findByUser_IdOrderByCreatedAtDesc(userId);
        return userNotifications.stream()
                .map(this::map)
                .toList();
    }

    /** Marks one notification as read. */
    @Transactional
    public NotificationResponse markRead(Long id, String email) {
        var notification = notifications.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found: " + id));
        requireOwner(notification.getUser().getEmail(), email);
        notification.setIsRead(true);
        return map(notification);
    }

    /** Returns the signed-in student's preference without accepting an arbitrary user ID. */
    @Transactional(readOnly = true)
    public EmailNotificationPreferenceResponse emailPreference(String email) {
        var user = student(email);
        return new EmailNotificationPreferenceResponse(
                Boolean.TRUE.equals(user.getEmailNotificationsEnabled()), emailDeliveryAvailable && !mailHost.isBlank());
    }

    /** Saves the signed-in student's opt-in/out choice. */
    @Transactional
    public EmailNotificationPreferenceResponse setEmailPreference(boolean enabled, String email) {
        var user = student(email);
        user.setEmailNotificationsEnabled(enabled);
        return new EmailNotificationPreferenceResponse(enabled, emailDeliveryAvailable && !mailHost.isBlank());
    }

    private void requireOwner(String ownerEmail, String authenticatedEmail) {
        if (!ownerEmail.equalsIgnoreCase(authenticatedEmail)) {
            throw new AccessDeniedException(
                    "This notification does not belong to the signed-in account");
        }
    }

    /** Saves a notification for a user, typically after an application event. */
    @Transactional
    public NotificationResponse create(User user, String title, String message, String type) {
        var notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        var saved = notifications.save(notification);
        if ("STUDENT".equalsIgnoreCase(user.getRole())
                && Boolean.TRUE.equals(user.getEmailNotificationsEnabled())) {
            var queued = new NotificationEmailOutbox();
            queued.setUser(user);
            queued.setNotification(saved);
            queued.setStatus("PENDING");
            queued.setAttempts(0);
            queued.setCreatedAt(LocalDateTime.now());
            queued.setNextAttemptAt(LocalDateTime.now());
            emailOutbox.save(queued);
        }
        return map(saved);
    }

    private User student(String email) {
        var user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Email notification preferences are only available to students");
        }
        return user;
    }

    private NotificationResponse map(Notification notification) {
        var response = new NotificationResponse();
        response.setId(notification.getId());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setType(notification.getType());
        response.setIsRead(notification.getIsRead());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }
}
