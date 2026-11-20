package com.proj.ScholarAssist;

import com.proj.ScholarAssist.entity.User;
import com.proj.ScholarAssist.repository.NotificationEmailOutboxRepository;
import com.proj.ScholarAssist.repository.UserRepository;
import com.proj.ScholarAssist.service.NotificationService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.UUID;

/** Uses existing accounts and rolls preference and notification changes back after each test. */
@SpringBootTest
class NotificationEmailTests {
    @Autowired private UserRepository users;
    @Autowired private NotificationEmailOutboxRepository outbox;
    @Autowired private NotificationService notifications;

    @Test
    @Transactional
    @Rollback
    void studentEmailAlertsAreOptInAndQueuedWithTheInAppNotification() {
        User student = users.findAll().stream()
                .filter(user -> "STUDENT".equalsIgnoreCase(user.getRole()))
                .findFirst().orElse(null);
        Assumptions.assumeTrue(student != null, "This check needs an existing student account");

        var initialPreference = notifications.setEmailPreference(false, student.getEmail());
        assertThat(initialPreference.enabled()).isFalse();
        assertThat(initialPreference.deliveryAvailable()).isFalse();
        String title = "Email outbox check " + UUID.randomUUID();

        notifications.create(student, title, "No external email is sent in this test.", "APPLICATION");
        assertThat(outbox.findAll()).noneMatch(item -> item.getNotification().getTitle().equals(title));

        notifications.setEmailPreference(true, student.getEmail());
        var created = notifications.create(student, title, "Details remain in app.", "APPLICATION");
        var queued = outbox.findByNotification_Id(created.getId()).orElseThrow();
        assertThat(queued.getUser().getId()).isEqualTo(student.getId());
        assertThat(queued.getStatus()).isEqualTo("PENDING");
        assertThat(queued.getAttempts()).isZero();
    }

    @Test
    void reviewerAccountsCannotChangeStudentEmailPreferences() {
        User reviewer = users.findAll().stream()
                .filter(user -> "REVIEWER".equalsIgnoreCase(user.getRole())
                        || "ADMIN".equalsIgnoreCase(user.getRole()))
                .findFirst().orElse(null);
        Assumptions.assumeTrue(reviewer != null, "This role check needs an existing reviewer account");

        assertThatThrownBy(() -> notifications.setEmailPreference(true, reviewer.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }
}
