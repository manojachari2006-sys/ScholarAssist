package com.proj.ScholarAssist;

import com.proj.ScholarAssist.entity.Application;
import com.proj.ScholarAssist.entity.User;
import com.proj.ScholarAssist.dto.ExternalApplicationRequest;
import com.proj.ScholarAssist.dto.ExternalApplicationSubmissionRequest;
import com.proj.ScholarAssist.repository.ApplicationRepository;
import com.proj.ScholarAssist.repository.ApplicationStatusHistoryRepository;
import com.proj.ScholarAssist.repository.ApplicationSupportRequestRepository;
import com.proj.ScholarAssist.repository.NoticationRepository;
import com.proj.ScholarAssist.repository.ScholarshipRepository;
import com.proj.ScholarAssist.repository.UserRepository;
import com.proj.ScholarAssist.service.ApplicationService;
import com.proj.ScholarAssist.service.NotificationService;
import com.proj.ScholarAssist.service.ScholarshipService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;

import java.util.List;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Exercises reviewer status updates and student-visible notices using existing local data. */
@SpringBootTest
class ReviewerApplicationWorkflowTests {
    private static final List<String> REVIEWABLE_STATUSES = List.of(
            "SUBMITTED", "APPLIED", "PENDING", "UNDER_REVIEW", "NEEDS_INFORMATION");

    @Autowired
    private UserRepository users;

    @Autowired
    private ApplicationRepository applications;

    @Autowired
    private ScholarshipRepository scholarships;

    @Autowired
    private ApplicationStatusHistoryRepository statusHistory;

    @Autowired
    private ApplicationSupportRequestRepository supportRequests;

    @Autowired
    private NoticationRepository notifications;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ScholarshipService scholarshipService;

    @Autowired
    private NotificationService notificationService;

    /** Education filters must honor explicit scholarship rules when the display column is blank. */
    @Test
    void educationFilterIncludesScholarshipsDefinedByEligibilityRules() {
        var nirankari = scholarships.findByStatusIgnoreCase("ACTIVE").stream()
                .filter(item -> "Nirankari Rajmata Scholarship Scheme 2026-27"
                        .equals(item.getName()))
                .filter(item -> item.getEducationLevel() == null
                        || item.getEducationLevel().isBlank())
                .findFirst().orElse(null);
        Assumptions.assumeTrue(nirankari != null,
                "This catalog check needs the active Nirankari row with rule-based education criteria");

        assertThat(scholarshipService.getRules(nirankari.getId()))
                .anySatisfy(rule -> {
                    assertThat(rule.getRuleType()).isEqualTo("EDUCATION_LEVEL");
                    assertThat(rule.getRuleValue()).contains("Undergraduate");
                });
        assertThat(scholarshipService.all(null, null, "UNDERGRADUATE"))
                .anySatisfy(item -> assertThat(item.getId()).isEqualTo(nirankari.getId()));
    }

    /** Uses a rollback-only transaction so no test status or notice remains in the database. */
    @Test
    @Transactional
    @Rollback
    void reviewerUpdateIsSavedAndVisibleToTheOwningStudent() throws Exception {
        User reviewer = users.findAll().stream()
                .filter(user -> List.of("REVIEWER", "ADMIN").contains(user.getRole()))
                .findFirst()
                .orElse(null);
        Application application = applications.findAllByOrderByUpdatedAtDesc().stream()
                .filter(item -> REVIEWABLE_STATUSES.contains(item.getStatus()))
                .findFirst()
                .orElse(null);

        Assumptions.assumeTrue(reviewer != null && application != null,
                "This workflow check needs an existing reviewer and active submitted application");

        String newStatus = "NEEDS_INFORMATION".equals(application.getStatus())
                ? "UNDER_REVIEW"
                : "NEEDS_INFORMATION";
        String message = "Rollback-only reviewer workflow check; no provider outcome asserted.";
        Long ownerUserId = application.getStudent().getUser().getId();
        String ownerEmail = application.getStudent().getUser().getEmail();
        Long applicationId = application.getId();

        applicationService.setStatus(
                applicationId, newStatus, message, reviewer.getEmail());

        entityManager.flush();
        entityManager.clear();

        Application persistedApplication = applications.findById(applicationId).orElseThrow();
        assertThat(persistedApplication.getStatus()).isEqualTo(newStatus);
        assertThat(statusHistory.findByApplication_IdOrderByChangedAtAscIdAsc(applicationId))
                .anySatisfy(entry -> {
                    assertThat(entry.getNewStatus()).isEqualTo(newStatus);
                    assertThat(entry.getNote()).isEqualTo(message);
                    assertThat(entry.getChangeSource()).isEqualTo("REVIEWER");
                });
        assertThat(notifications.findByUser_IdOrderByCreatedAtDesc(ownerUserId))
                .anySatisfy(notification -> {
                    assertThat(notification.getMessage()).contains(message);
                    assertThat(notification.getType()).isEqualTo("APPLICATION");
                });

        assertThat(notificationService.byUser(ownerUserId, false, ownerEmail))
                .anySatisfy(notification -> assertThat(notification.getMessage())
                        .contains(message));
        assertThat(applicationService.history(applicationId, ownerEmail))
                .anySatisfy(entry -> assertThat(entry.note()).isEqualTo(message));
    }

    /** Exercises one real persisted tracker row from student submission through reviewer update. */
    @Test
    @Transactional
    @Rollback
    void studentSubmissionAndReviewerUpdateUseTheSameApplicationRecord() {
        User student = users.findAll().stream()
                .filter(user -> "STUDENT".equals(user.getRole())
                        && user.getStudentProfile() != null)
                .findFirst().orElse(null);
        User reviewer = users.findAll().stream()
                .filter(user -> List.of("REVIEWER", "ADMIN").contains(user.getRole()))
                .findFirst().orElse(null);
        var scholarship = scholarships.findByStatusIgnoreCase("ACTIVE").stream()
                .filter(item -> item.getDeadline() == null
                        || !item.getDeadline().isBefore(LocalDate.now()))
                .filter(item -> student != null && !applications
                        .existsByStudent_IdAndScholarship_Id(
                                student.getStudentProfile().getId(), item.getId()))
                .findFirst().orElse(null);

        Assumptions.assumeTrue(student != null && reviewer != null && scholarship != null,
                "This rollback-only workflow check needs an existing student, reviewer, and open scholarship");

        String ownerEmail = student.getEmail();
        String reviewerEmail = reviewer.getEmail();
        Long ownerUserId = student.getId();
        var started = applicationService.startExternalApplication(
                student.getStudentProfile().getId(),
                new ExternalApplicationRequest(scholarship.getId()), ownerEmail);
        assertThat(started.getStatus()).isEqualTo("EXTERNAL_STARTED");

        var confirmed = applicationService.confirmExternalSubmission(
                started.getId(),
                new ExternalApplicationSubmissionRequest(
                        " TEST-ONGC-12345 ", " Student supplied note "),
                ownerEmail);
        Long applicationId = confirmed.getId();
        assertThat(confirmed.getProviderApplicationId()).isEqualTo("TEST-ONGC-12345");
        assertThat(confirmed.getStatus()).isEqualTo("APPLIED");
        assertThat(confirmed.getSubmittedAt()).isNotNull();
        int historyCountAfterConfirmation = statusHistory
                .findByApplication_IdOrderByChangedAtAscIdAsc(started.getId()).size();
        var duplicate = applicationService.confirmExternalSubmission(started.getId(),
                new ExternalApplicationSubmissionRequest(
                        "TEST-ONGC-12345", "Student supplied note"), ownerEmail);
        assertThat(duplicate.getId()).isEqualTo(started.getId());
        assertThat(statusHistory.findByApplication_IdOrderByChangedAtAscIdAsc(started.getId()))
                .hasSize(historyCountAfterConfirmation);

        var queued = applicationService.reviewQueue(reviewerEmail).stream()
                .filter(item -> item.id().equals(applicationId)).findFirst().orElseThrow();
        assertThat(queued.providerApplicationId()).isEqualTo("TEST-ONGC-12345");
        assertThat(queued.studentEmail()).isEqualTo(ownerEmail);

        applicationService.setStatus(
                applicationId, "UNDER_REVIEW", "Reviewer test update", reviewerEmail);
        entityManager.flush();
        entityManager.clear();

        Application persisted = applications.findById(applicationId).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo("UNDER_REVIEW");
        assertThat(persisted.getProviderApplicationId()).isEqualTo("TEST-ONGC-12345");
        assertThat(persisted.getRemarks()).isEqualTo("Student supplied note");
        assertThat(applicationService.reviewerDetails(applicationId, reviewerEmail)
                .providerApplicationId()).isEqualTo("TEST-ONGC-12345");
        assertThat(applicationService.history(applicationId, ownerEmail))
                .anySatisfy(entry -> {
                    assertThat(entry.newStatus()).isEqualTo("UNDER_REVIEW");
                    assertThat(entry.note()).isEqualTo("Reviewer test update");
                    assertThat(entry.changeSource()).isEqualTo("REVIEWER");
                });
        assertThat(notifications.findByUser_IdOrderByCreatedAtDesc(ownerUserId))
                .anySatisfy(notification -> assertThat(notification.getMessage())
                        .contains("Reviewer test update"));

    }

    /** Student credentials cannot access reviewer queues or another student's tracker. */
    @Test
    void studentCannotReadReviewerQueueOrAnotherStudentsApplication() throws Exception {
        User student = users.findAll().stream()
                .filter(user -> "STUDENT".equals(user.getRole())
                        && user.getStudentProfile() != null)
                .findFirst()
                .orElse(null);
        Application otherStudentsApplication = applications.findAllByOrderByUpdatedAtDesc().stream()
                .filter(item -> student != null
                        && !item.getStudent().getUser().getId().equals(student.getId()))
                .findFirst()
                .orElse(null);

        Assumptions.assumeTrue(student != null && otherStudentsApplication != null,
                "This access check needs two existing students and an application");

        assertThatThrownBy(() -> applicationService.reviewQueue(student.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> applicationService.reviewerDetails(
                        otherStudentsApplication.getId(), student.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    /** Verifies a cancellation decision changes the tracker, history, and notice atomically. */
    @Test
    @Transactional
    @Rollback
    void approvedCancellationRequestClosesOnlyItsApplicationAndNotifiesItsOwner() {
        User reviewer = users.findAll().stream()
                .filter(user -> List.of("REVIEWER", "ADMIN").contains(user.getRole()))
                .findFirst().orElse(null);
        Application application = applications.findAllByOrderByUpdatedAtDesc().stream()
                .filter(item -> List.of("EXTERNAL_STARTED", "APPLIED").contains(item.getStatus()))
                .filter(item -> item.getScholarship().getDeadline() == null
                        || !item.getScholarship().getDeadline().isBefore(LocalDate.now()))
                .filter(item -> !supportRequests.existsByApplication_IdAndStatus(
                        item.getId(), "PENDING"))
                .findFirst().orElse(null);
        Assumptions.assumeTrue(reviewer != null && application != null,
                "This rollback-only check needs a reviewer and an open, unrequested tracker");

        Long applicationId = application.getId();
        Long ownerId = application.getStudent().getUser().getId();
        String ownerEmail = application.getStudent().getUser().getEmail();
        var created = applicationService.createSupportRequest(
                applicationId, "CANCELLATION", "Please stop tracking this application.", ownerEmail);
        assertThat(created.status()).isEqualTo("PENDING");
        assertThat(applicationService.supportRequestsForApplication(applicationId, ownerEmail))
                .extracting(item -> item.id()).contains(created.id());

        var decided = applicationService.decideSupportRequest(
                created.id(), "APPROVED", "Cancellation recorded in ScholarAssist.", reviewer.getEmail());
        entityManager.flush();
        entityManager.clear();

        assertThat(decided.status()).isEqualTo("APPROVED");
        assertThat(applications.findById(applicationId).orElseThrow().getStatus())
                .isEqualTo("CANCELLED");
        assertThat(statusHistory.findByApplication_IdOrderByChangedAtAscIdAsc(applicationId))
                .anySatisfy(entry -> {
                    assertThat(entry.getNewStatus()).isEqualTo("CANCELLED");
                    assertThat(entry.getChangeSource()).isEqualTo("REVIEWER");
                });
        assertThat(notifications.findByUser_IdOrderByCreatedAtDesc(ownerId))
                .anySatisfy(item -> assertThat(item.getMessage()).contains("was approved"));
    }
}
