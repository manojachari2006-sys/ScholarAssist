package com.proj.ScholarAssist.service;

import com.proj.ScholarAssist.entity.NotificationEmailOutbox;
import com.proj.ScholarAssist.repository.NotificationEmailOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Sends queued transactional alerts only when SMTP is explicitly enabled and configured. */
@Service
@ConditionalOnProperty(name = "scholarassist.email.enabled", havingValue = "true")
public class NotificationEmailDeliveryProcessor {
    private static final Logger log = LoggerFactory.getLogger(NotificationEmailDeliveryProcessor.class);
    private static final int MAX_ATTEMPTS = 5;

    private final NotificationEmailOutboxRepository outbox;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String from;
    private final String frontendUrl;

    public NotificationEmailDeliveryProcessor(
            NotificationEmailOutboxRepository outbox,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${scholarassist.email.from:ScholarAssist <no-reply@localhost>}") String from,
            @Value("${scholarassist.email.frontend-url:http://localhost:5500}") String frontendUrl) {
        this.outbox = outbox;
        this.mailSenderProvider = mailSenderProvider;
        this.from = from;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Transactional
    public void deliverOne(Long id) {
        var queued = outbox.findByIdForUpdate(id).orElse(null);
        if (queued == null || !"PENDING".equals(queued.getStatus())) return;

        var user = queued.getUser();
        if (!Boolean.TRUE.equals(user.getEmailNotificationsEnabled())
                || !"STUDENT".equalsIgnoreCase(user.getRole())) {
            queued.setStatus("SKIPPED");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("ScholarAssist update: " + safeSubject(queued.getNotification().getTitle()));
        message.setText("There is a new update in your ScholarAssist account.\n\n"
                + "Sign in to view the message in your notifications: "
                + frontendUrl + "/pages/notifications.html\n\n"
                + "This email contains no application or document details. You received it because "
                + "email notifications are enabled in your ScholarAssist account.");
        try {
            var mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) return;
            mailSender.send(message);
            queued.setStatus("SENT");
            queued.setSentAt(LocalDateTime.now());
            queued.setLastError(null);
        } catch (MailException failure) {
            int attempts = queued.getAttempts() + 1;
            queued.setAttempts(attempts);
            queued.setLastError(failure.getClass().getSimpleName());
            if (attempts >= MAX_ATTEMPTS) {
                queued.setStatus("FAILED");
            } else {
                queued.setStatus("PENDING");
                queued.setNextAttemptAt(LocalDateTime.now().plusMinutes(Math.min(60, 1L << attempts)));
            }
            log.warn("Notification email delivery failed for outbox item {}; attempt {} of {}",
                    id, attempts, MAX_ATTEMPTS);
        }
    }

    private String safeSubject(String title) {
        if (title == null || title.isBlank()) return "Account notification";
        return title.replaceAll("[\\r\\n]+", " ").trim();
    }
}
