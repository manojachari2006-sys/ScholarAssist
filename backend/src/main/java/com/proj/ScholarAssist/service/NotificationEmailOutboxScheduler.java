package com.proj.ScholarAssist.service;

import com.proj.ScholarAssist.repository.NotificationEmailOutboxRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Periodically dispatches a small batch of committed email notifications. */
@Component
@ConditionalOnProperty(name = "scholarassist.email.enabled", havingValue = "true")
public class NotificationEmailOutboxScheduler {
    private final NotificationEmailOutboxRepository outbox;
    private final NotificationEmailDeliveryProcessor processor;
    private final org.springframework.beans.factory.ObjectProvider<JavaMailSender> mailSenderProvider;

    public NotificationEmailOutboxScheduler(
            NotificationEmailOutboxRepository outbox,
            NotificationEmailDeliveryProcessor processor,
            org.springframework.beans.factory.ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.outbox = outbox;
        this.processor = processor;
        this.mailSenderProvider = mailSenderProvider;
    }

    @Scheduled(fixedDelayString = "${scholarassist.email.poll-interval-ms:30000}")
    public void deliverDueEmails() {
        if (mailSenderProvider.getIfAvailable() == null) return;
        outbox.findDueIds(LocalDateTime.now(), PageRequest.of(0, 20))
                .forEach(processor::deliverOne);
    }
}
