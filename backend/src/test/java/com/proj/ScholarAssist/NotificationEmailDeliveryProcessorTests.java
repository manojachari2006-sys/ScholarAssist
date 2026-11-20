package com.proj.ScholarAssist;

import com.proj.ScholarAssist.entity.Notification;
import com.proj.ScholarAssist.entity.NotificationEmailOutbox;
import com.proj.ScholarAssist.entity.User;
import com.proj.ScholarAssist.repository.NotificationEmailOutboxRepository;
import com.proj.ScholarAssist.service.NotificationEmailDeliveryProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Exercises email composition with a mocked sender; no network email is delivered. */
class NotificationEmailDeliveryProcessorTests {
    @Test
    @SuppressWarnings("unchecked")
    void sendsOnlyAGenericLinkToTheInAppMessage() {
        var outbox = mock(NotificationEmailOutboxRepository.class);
        var senderProvider = mock(ObjectProvider.class);
        var sender = mock(JavaMailSender.class);
        when(senderProvider.getIfAvailable()).thenReturn(sender);

        var student = new User();
        student.setId(41L);
        student.setEmail("student@example.test");
        student.setRole("STUDENT");
        student.setEmailNotificationsEnabled(true);

        var notification = new Notification();
        notification.setTitle("Application update");
        notification.setMessage("Private reviewer message containing application details");

        var queued = new NotificationEmailOutbox();
        queued.setId(12L);
        queued.setUser(student);
        queued.setNotification(notification);
        queued.setStatus("PENDING");
        queued.setCreatedAt(LocalDateTime.now());
        queued.setNextAttemptAt(LocalDateTime.now());
        when(outbox.findByIdForUpdate(12L)).thenReturn(Optional.of(queued));

        var processor = new NotificationEmailDeliveryProcessor(
                outbox, senderProvider, "alerts@example.test", "http://localhost:5500");
        processor.deliverOne(12L);

        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("student@example.test");
        assertThat(captor.getValue().getSubject()).contains("Application update");
        assertThat(captor.getValue().getText()).contains("/pages/notifications.html")
                .doesNotContain("Private reviewer message", "application details");
        assertThat(queued.getStatus()).isEqualTo("SENT");
        assertThat(queued.getSentAt()).isNotNull();
    }
}
