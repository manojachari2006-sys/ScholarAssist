package com.proj.ScholarAssist.service;

import com.proj.ScholarAssist.dto.ApplicationRequest;
import com.proj.ScholarAssist.dto.ApplicationResponse;
import com.proj.ScholarAssist.dto.ApplicationStatusHistoryResponse;
import com.proj.ScholarAssist.dto.ApplicationSupportRequestResponse;
import com.proj.ScholarAssist.dto.ExternalApplicationRequest;
import com.proj.ScholarAssist.dto.ExternalApplicationSubmissionRequest;
import com.proj.ScholarAssist.dto.ReviewerApplicationResponse;
import com.proj.ScholarAssist.entity.Application;
import com.proj.ScholarAssist.entity.ApplicationDocument;
import com.proj.ScholarAssist.entity.ApplicationStatusHistory;
import com.proj.ScholarAssist.entity.ApplicationSupportRequest;
import com.proj.ScholarAssist.entity.User;
import com.proj.ScholarAssist.exception.DuplicateResourceException;
import com.proj.ScholarAssist.exception.ResourceNotFoundException;
import com.proj.ScholarAssist.repository.ApplicationDocumentRepository;
import com.proj.ScholarAssist.repository.ApplicationRepository;
import com.proj.ScholarAssist.repository.ApplicationStatusHistoryRepository;
import com.proj.ScholarAssist.repository.ApplicationSupportRequestRepository;
import com.proj.ScholarAssist.repository.DocumentRepository;
import com.proj.ScholarAssist.repository.ScholarshipRepository;
import com.proj.ScholarAssist.repository.StudentProfileRepository;
import com.proj.ScholarAssist.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

/** Coordinates application lifecycle changes and required document links. */
@Service
public class ApplicationService {
    private final ApplicationRepository applications;
    private final StudentProfileRepository students;
    private final ScholarshipRepository scholarships;
    private final DocumentRepository documents;
    private final ApplicationDocumentRepository applicationDocuments;
    private final NotificationService notifications;
    private final ApplicationStatusHistoryRepository statusHistory;
    private final ApplicationSupportRequestRepository supportRequests;
    private final UserRepository users;

    public ApplicationService(
            ApplicationRepository applications,
            StudentProfileRepository students,
            ScholarshipRepository scholarships,
            DocumentRepository documents,
            ApplicationDocumentRepository applicationDocuments,
            NotificationService notifications,
            ApplicationStatusHistoryRepository statusHistory,
            ApplicationSupportRequestRepository supportRequests,
            UserRepository users) {
        this.applications = applications;
        this.students = students;
        this.scholarships = scholarships;
        this.documents = documents;
        this.applicationDocuments = applicationDocuments;
        this.notifications = notifications;
        this.statusHistory = statusHistory;
        this.supportRequests = supportRequests;
        this.users = users;
    }

    /** Creates a DRAFT application after checking its student and scholarship. */
    @Transactional
    public ApplicationResponse create(Long studentId, ApplicationRequest request, String email) {
        // Lock the owning profile so two concurrent requests cannot both pass the duplicate check.
        var student = students.findByIdForUpdate(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student profile not found: " + studentId));
        requireOwner(student.getUser().getEmail(), email);
        var scholarship = scholarships.findById(request.getScholarshipId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Scholarship not found: " + request.getScholarshipId()));

        if (!"ACTIVE".equalsIgnoreCase(scholarship.getStatus())
                || (scholarship.getDeadline() != null
                && scholarship.getDeadline().isBefore(LocalDate.now()))) {
            throw new IllegalArgumentException(
                    "This scholarship is no longer accepting applications");
        }

        if (applications.existsByStudent_IdAndScholarship_Id(
                studentId, scholarship.getId())) {
            throw new DuplicateResourceException(
                    "An application already exists for this student and scholarship");
        }

        var application = new Application();
        application.setStudent(student);
        application.setScholarship(scholarship);
        application.setApplicationDate(request.getApplicationDate());
        application.setRemarks(request.getRemarks());
        application.setStatus("DRAFT");
        application.setCreatedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        application = applications.save(application);
        recordStatus(application, null, "DRAFT", "Draft created in ScholarAssist.",
                "STUDENT", student.getUser());
        return map(application);
    }

    /**
     * Creates or reuses a local tracker record before opening a provider's external form.
     * The record indicates the student started the provider flow, not that they submitted it.
     */
    @Transactional
    public ApplicationResponse startExternalApplication(
            Long studentId,
            ExternalApplicationRequest request,
            String email) {
        var student = students.findByIdForUpdate(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student profile not found: " + studentId));
        requireOwner(student.getUser().getEmail(), email);
        var scholarship = scholarships.findById(request.scholarshipId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Scholarship not found: " + request.scholarshipId()));
        requireOpenScholarship(scholarship);

        var existing = applications.findByStudent_IdAndScholarship_Id(
                studentId, scholarship.getId());
        if (existing.isPresent()) {
            var application = existing.get();
            if ("DRAFT".equals(application.getStatus())) {
                changeStudentStatus(
                        application, "EXTERNAL_STARTED",
                        "Student opened the provider's external application.");
            }
            return map(application);
        }

        var application = new Application();
        application.setStudent(student);
        application.setScholarship(scholarship);
        application.setApplicationDate(LocalDate.now());
        application.setStatus("EXTERNAL_STARTED");
        application.setCreatedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        application = applications.save(application);
        recordStatus(application, null, "EXTERNAL_STARTED",
                "Student opened the provider's external application.",
                "STUDENT", student.getUser());
        return map(application);
    }

    /** Records the student's confirmation that they submitted on the provider's site. */
    @Transactional
    public ApplicationResponse confirmExternalSubmission(
            Long id,
            ExternalApplicationSubmissionRequest request,
            String email) {
        var application = applications.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
        requireOwner(application.getStudent().getUser().getEmail(), email);
        String providerReference = cleanProviderReference(request.providerApplicationId());
        String studentNote = clean(request.remarks());
        if ("APPLIED".equals(application.getStatus())) {
            boolean unchangedReference = providerReference == null
                    || providerReference.equals(application.getProviderApplicationId());
            boolean unchangedNote = studentNote == null
                    || studentNote.equals(application.getRemarks());
            if (unchangedReference && unchangedNote) {
                return map(application);
            }
            boolean reviewerHasUpdated = statusHistory
                    .findTopByApplication_IdAndChangeSourceOrderByChangedAtDescIdDesc(id, "REVIEWER")
                    .isPresent();
            if (reviewerHasUpdated) {
                throw new IllegalArgumentException(
                        "Provider details can only be corrected before reviewer updates begin");
            }
            if (providerReference != null) {
                application.setProviderApplicationId(providerReference);
            }
            if (studentNote != null) {
                application.setRemarks(studentNote);
            }
            application.setUpdatedAt(LocalDateTime.now());
            recordStatus(application, "APPLIED", "APPLIED",
                    "Student corrected external application details.",
                    "STUDENT", application.getStudent().getUser());
            return map(application);
        }
        if (!"EXTERNAL_STARTED".equals(application.getStatus())) {
            throw new IllegalArgumentException(
                    "Only an externally started application can be marked submitted");
        }

        application.setStatus("APPLIED");
        application.setSubmittedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        application.setProviderApplicationId(providerReference);
        if (studentNote != null) {
            application.setRemarks(studentNote);
        }
        recordStatus(application, "EXTERNAL_STARTED", "APPLIED",
                "Student confirmed submission on the provider's website.",
                "STUDENT", application.getStudent().getUser());
        notifications.create(
                application.getStudent().getUser(),
                "Application marked as submitted",
                "You marked your " + application.getScholarship().getName()
                        + " application as submitted. Provider decisions are not synced automatically.",
                "APPLICATION");
        return map(application);
    }

    /** Lists all applications for an existing student profile. */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> byStudent(Long studentId, String email) {
        var student = students.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student profile not found: " + studentId));
        requireOwner(student.getUser().getEmail(), email);
        var studentApplications = applications.findByStudent_Id(studentId);
        if (studentApplications.isEmpty()) {
            return List.of();
        }
        var applicationIds = studentApplications.stream().map(Application::getId).toList();
        var latestByApplication = statusHistory.findLatestForApplications(applicationIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> entry.getApplication().getId(), entry -> entry));
        return studentApplications.stream().map(application -> {
            var response = map(application);
            var latest = latestByApplication.get(application.getId());
            if (latest != null) {
                response.setLatestUpdate(latest.getNote());
                response.setLatestUpdateSource(latest.getChangeSource());
                response.setLatestUpdateAt(latest.getChangedAt());
            }
            return response;
        }).toList();
    }

    /** Retrieves an application by ID. */
    @Transactional(readOnly = true)
    public ApplicationResponse get(Long id, String email) {
        var application = require(id);
        requireOwner(application.getStudent().getUser().getEmail(), email);
        return map(application);
    }

    /** Updates remarks while the application is still a draft. */
    @Transactional
    public ApplicationResponse updateRemarks(Long id, String remarks, String email) {
        var application = require(id);
        requireOwner(application.getStudent().getUser().getEmail(), email);
        if (!"DRAFT".equals(application.getStatus())) {
            throw new IllegalArgumentException("Only draft applications can be edited");
        }

        application.setRemarks(remarks);
        application.setUpdatedAt(LocalDateTime.now());
        return map(application);
    }

    /** Submits a draft after all required linked documents are marked submitted. */
    @Transactional
    public ApplicationResponse submit(Long id, String email) {
        var application = require(id);
        requireOwner(application.getStudent().getUser().getEmail(), email);
        if (!"DRAFT".equals(application.getStatus())) {
            throw new IllegalArgumentException("Only draft applications can be submitted");
        }

        var requiredDocuments = applicationDocuments.findByApplication_Id(id).stream()
                .filter(link -> Boolean.TRUE.equals(link.getRequired()))
                .toList();
        boolean hasUnsubmittedRequiredDocument = requiredDocuments.stream()
                .anyMatch(link -> !Boolean.TRUE.equals(link.getSubmitted()));
        if (hasUnsubmittedRequiredDocument) {
            throw new IllegalArgumentException(
                    "All required documents must be marked submitted");
        }

        application.setStatus("SUBMITTED");
        application.setSubmittedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        recordStatus(application, "DRAFT", "SUBMITTED",
                "Student submitted the ScholarAssist application draft.",
                "STUDENT", application.getStudent().getUser());
        notifications.create(
                application.getStudent().getUser(),
                "Application submitted",
                "Your application for " + application.getScholarship().getName() + " was submitted.",
                "APPLICATION");
        return map(application);
    }

    /** Applies a supported review status and notifies the student. */
    @Transactional
    public ApplicationResponse setStatus(
            Long id, String requestedStatus, String remarks, String reviewerEmail) {
        var reviewer = reviewer(reviewerEmail);
        if (requestedStatus == null || requestedStatus.isBlank()) {
            throw new IllegalArgumentException("Application status is required");
        }
        String status = requestedStatus.trim().toUpperCase(java.util.Locale.ROOT);
        var application = applications.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
        if (!List.of("PENDING", "UNDER_REVIEW", "NEEDS_INFORMATION", "APPROVED", "REJECTED")
                .contains(status)) {
            throw new IllegalArgumentException("Unsupported application status");
        }
        if (!List.of("SUBMITTED", "APPLIED", "PENDING", "UNDER_REVIEW", "NEEDS_INFORMATION")
                .contains(application.getStatus())) {
            throw new IllegalArgumentException(
                    "Only submitted or active-review applications can change review status");
        }

        String previousStatus = application.getStatus();
        String updateMessage = clean(remarks);
        var lastReviewerEntry = statusHistory
                .findTopByApplication_IdAndChangeSourceOrderByChangedAtDescIdDesc(id, "REVIEWER")
                .orElse(null);
        if (status.equals(previousStatus)
                && (updateMessage == null || (lastReviewerEntry != null
                && updateMessage.equals(lastReviewerEntry.getNote())))) {
            throw new IllegalArgumentException(
                    "Enter a new provider update or choose a different status");
        }
        application.setStatus(status);
        application.setUpdatedAt(LocalDateTime.now());
        recordStatus(application, previousStatus, status, updateMessage, "REVIEWER", reviewer);
        notifications.create(
                application.getStudent().getUser(),
                "Scholarship application update",
                "Your application for " + application.getScholarship().getName()
                        + " is now " + displayStatus(status) + "."
                        + (updateMessage == null ? "" : " Update: " + updateMessage),
                "APPLICATION");
        return map(application);
    }

    /** Creates a student-owned correction or cancellation request before the scholarship deadline. */
    @Transactional
    public ApplicationSupportRequestResponse createSupportRequest(
            Long applicationId, String requestedType, String message, String studentEmail) {
        var application = applications.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        requireOwner(application.getStudent().getUser().getEmail(), studentEmail);
        String type = requestedType == null ? "" : requestedType.trim().toUpperCase(java.util.Locale.ROOT);
        String normalizedMessage = clean(message);
        if (!List.of("CORRECTION", "CANCELLATION").contains(type)) {
            throw new IllegalArgumentException("Request type must be CORRECTION or CANCELLATION");
        }
        if (normalizedMessage == null || normalizedMessage.length() > 2000) {
            throw new IllegalArgumentException("Explain your request in 1 to 2000 characters");
        }
        if (!List.of("EXTERNAL_STARTED", "APPLIED").contains(application.getStatus())) {
            throw new IllegalArgumentException("Requests are available only before a final provider decision");
        }
        if ("CORRECTION".equals(type) && !"APPLIED".equals(application.getStatus())) {
            throw new IllegalArgumentException("A correction request is available after you report provider submission");
        }
        ensureBeforeDeadline(application);
        if (supportRequests.existsByApplication_IdAndStatus(applicationId, "PENDING")) {
            throw new IllegalArgumentException("A request is already waiting for reviewer action");
        }

        var request = new ApplicationSupportRequest();
        request.setApplication(application);
        request.setRequestedBy(application.getStudent().getUser());
        request.setRequestType(type);
        request.setStudentMessage(normalizedMessage);
        request.setStatus("PENDING");
        request.setRequestedAt(LocalDateTime.now());
        request = supportRequests.save(request);
        return mapSupportRequest(request);
    }

    /** Lists a student's own application requests, or the same requests for an authorized reviewer. */
    @Transactional(readOnly = true)
    public List<ApplicationSupportRequestResponse> supportRequestsForApplication(
            Long applicationId, String email) {
        var application = require(applicationId);
        if (!application.getStudent().getUser().getEmail().equalsIgnoreCase(email)) {
            reviewer(email);
        }
        return supportRequests.findByApplication_IdOrderByRequestedAtDescIdDesc(applicationId)
                .stream().map(this::mapSupportRequest).toList();
    }

    /** Returns pending student requests to reviewers/admins. */
    @Transactional(readOnly = true)
    public List<ApplicationSupportRequestResponse> pendingSupportRequests(String reviewerEmail) {
        reviewer(reviewerEmail);
        return supportRequests.findByStatusOrderByRequestedAtAscIdAsc("PENDING")
                .stream().map(this::mapSupportRequest).toList();
    }

    /** Records a reviewer decision; approving cancellation also closes the tracked application. */
    @Transactional
    public ApplicationSupportRequestResponse decideSupportRequest(
            Long requestId, String requestedDecision, String message, String reviewerEmail) {
        User reviewer = reviewer(reviewerEmail);
        var request = supportRequests.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Support request not found: " + requestId));
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException("This request has already been reviewed");
        }
        String decision = requestedDecision == null ? "" : requestedDecision.trim().toUpperCase(java.util.Locale.ROOT);
        String reviewerMessage = clean(message);
        if (!List.of("APPROVED", "DECLINED").contains(decision)) {
            throw new IllegalArgumentException("Decision must be APPROVED or DECLINED");
        }
        if (reviewerMessage == null || reviewerMessage.length() > 2000) {
            throw new IllegalArgumentException("A reviewer response of 1 to 2000 characters is required");
        }
        Application application = applications.findByIdForUpdate(request.getApplication().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if ("APPROVED".equals(decision) && "CANCELLATION".equals(request.getRequestType())) {
            ensureBeforeDeadline(application);
            if (!List.of("EXTERNAL_STARTED", "APPLIED").contains(application.getStatus())) {
                throw new IllegalArgumentException("This application can no longer be cancelled through ScholarAssist");
            }
            String oldStatus = application.getStatus();
            application.setStatus("CANCELLED");
            application.setUpdatedAt(LocalDateTime.now());
            recordStatus(application, oldStatus, "CANCELLED",
                    "Reviewer approved the student's cancellation request.", "REVIEWER", reviewer);
        }
        request.setStatus(decision);
        request.setReviewer(reviewer);
        request.setReviewerMessage(reviewerMessage);
        request.setReviewedAt(LocalDateTime.now());
        supportRequests.save(request);
        String title = "Application support request update";
        String messageForStudent = "Your " + request.getRequestType().toLowerCase(java.util.Locale.ROOT)
                + " request for " + application.getScholarship().getName() + " was "
                + decision.toLowerCase(java.util.Locale.ROOT) + ". " + reviewerMessage;
        if ("APPROVED".equals(decision) && "CORRECTION".equals(request.getRequestType())) {
            messageForStudent += " ScholarAssist cannot edit the provider's external form; follow the provider's instructions to make the correction.";
        }
        notifications.create(application.getStudent().getUser(), title, messageForStudent, "APPLICATION");
        return mapSupportRequest(request);
    }

    /** Lists all tracked applications for the reviewer queue. */
    @Transactional(readOnly = true)
    public List<ReviewerApplicationResponse> reviewQueue(String reviewerEmail) {
        reviewer(reviewerEmail);
        return applications.findByStatusInOrderByUpdatedAtDesc(List.of(
                        "SUBMITTED", "APPLIED", "PENDING", "UNDER_REVIEW", "NEEDS_INFORMATION"))
                .stream()
                .map(application -> new ReviewerApplicationResponse(
                        application.getId(),
                        application.getStudent().getId(),
                        application.getStudent().getFullName(),
                        application.getStudent().getUser().getEmail(),
                        application.getScholarship().getName(),
                        application.getScholarship().getProvider(),
                        application.getStatus(),
                        application.getProviderApplicationId(),
                        application.getApplicationDate(),
                        application.getSubmittedAt(),
                        application.getUpdatedAt(),
                        application.getRemarks()))
                .toList();
    }

    /** Loads one application for the authorized reviewer detail view. */
    @Transactional(readOnly = true)
    public ReviewerApplicationResponse reviewerDetails(Long id, String reviewerEmail) {
        reviewer(reviewerEmail);
        var application = require(id);
        return new ReviewerApplicationResponse(
                application.getId(),
                application.getStudent().getId(),
                application.getStudent().getFullName(),
                application.getStudent().getUser().getEmail(),
                application.getScholarship().getName(),
                application.getScholarship().getProvider(),
                application.getStatus(),
                application.getProviderApplicationId(),
                application.getApplicationDate(),
                application.getSubmittedAt(),
                application.getUpdatedAt(),
                application.getRemarks());
    }

    /** Returns the audit timeline only to the owning student or an authorized reviewer. */
    @Transactional(readOnly = true)
    public List<ApplicationStatusHistoryResponse> history(Long id, String email) {
        var application = require(id);
        if (!application.getStudent().getUser().getEmail().equalsIgnoreCase(email)) {
            reviewer(email);
        }
        return statusHistory.findByApplication_IdOrderByChangedAtAscIdAsc(id).stream()
                .map(this::mapHistory)
                .toList();
    }

    /** Links a student's existing document to an application. */
    @Transactional
    public void associateDocument(Long applicationId, Long documentId, Boolean required) {
        var application = require(applicationId);
        var document = documents.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + documentId));

        if (!document.getStudent().getId().equals(application.getStudent().getId())) {
            throw new IllegalArgumentException("Document belongs to a different student");
        }
        if (applicationDocuments.findByApplication_IdAndDocument_Id(
                applicationId, documentId).isPresent()) {
            throw new DuplicateResourceException(
                    "Document is already associated with this application");
        }

        var link = new ApplicationDocument();
        link.setApplication(application);
        link.setDocument(document);
        link.setRequired(required == null || required);
        link.setSubmitted(false);
        applicationDocuments.save(link);
    }

    /** Associates a document only when the signed-in account owns the application. */
    @Transactional
    public void associateDocumentForUser(
            Long applicationId, Long documentId, Boolean required, String email) {
        var application = require(applicationId);
        if (!application.getStudent().getUser().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("This application does not belong to the signed-in account");
        }
        associateDocument(applicationId, documentId, required);
    }

    private void requireOwner(String ownerEmail, String authenticatedEmail) {
        if (!ownerEmail.equalsIgnoreCase(authenticatedEmail)) {
            throw new AccessDeniedException(
                    "This application does not belong to the signed-in account");
        }
    }

    /** Updates the submitted flag on an existing application-document link. */
    @Transactional
    public void markDocumentSubmitted(Long applicationId, Long documentId, boolean submitted) {
        var link = applicationDocuments.findByApplication_IdAndDocument_Id(
                        applicationId, documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application document link not found"));
        link.setSubmitted(submitted);
    }

    private Application require(Long id) {
        return applications.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    private ApplicationResponse map(Application application) {
        var response = new ApplicationResponse();
        response.setId(application.getId());
        response.setScholarshipId(application.getScholarship().getId());
        response.setScholarshipName(application.getScholarship().getName());
        response.setScholarshipDeadline(application.getScholarship().getDeadline());
        response.setApplicationDate(application.getApplicationDate());
        response.setStatus(application.getStatus());
        response.setRemarks(application.getRemarks());
        response.setProviderApplicationId(application.getProviderApplicationId());
        response.setSubmittedAt(application.getSubmittedAt());
        response.setCreatedAt(application.getCreatedAt());
        response.setUpdatedAt(application.getUpdatedAt());
        return response;
    }

    private void requireOpenScholarship(com.proj.ScholarAssist.entity.Scholarship scholarship) {
        if (!"ACTIVE".equalsIgnoreCase(scholarship.getStatus())
                || (scholarship.getDeadline() != null
                && scholarship.getDeadline().isBefore(LocalDate.now()))) {
            throw new IllegalArgumentException(
                    "This scholarship is no longer accepting applications");
        }
    }

    private void changeStudentStatus(
            Application application, String status, String note) {
        String previousStatus = application.getStatus();
        application.setStatus(status);
        application.setUpdatedAt(LocalDateTime.now());
        recordStatus(application, previousStatus, status, note,
                "STUDENT", application.getStudent().getUser());
    }

    private User reviewer(String email) {
        var user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer account not found"));
        if (!List.of("REVIEWER", "ADMIN").contains(user.getRole())) {
            throw new AccessDeniedException("Reviewer or admin role is required");
        }
        return user;
    }

    private void recordStatus(
            Application application,
            String previousStatus,
            String newStatus,
            String note,
            String source,
            com.proj.ScholarAssist.entity.User changedBy) {
        var historyEntry = new ApplicationStatusHistory();
        historyEntry.setApplication(application);
        historyEntry.setPreviousStatus(previousStatus);
        historyEntry.setNewStatus(newStatus);
        historyEntry.setNote(note);
        historyEntry.setChangeSource(source);
        historyEntry.setChangedBy(changedBy);
        historyEntry.setChangedAt(LocalDateTime.now());
        statusHistory.save(historyEntry);
    }

    private ApplicationStatusHistoryResponse mapHistory(ApplicationStatusHistory entry) {
        return new ApplicationStatusHistoryResponse(
                entry.getId(),
                entry.getPreviousStatus(),
                entry.getNewStatus(),
                entry.getNote(),
                entry.getChangeSource(),
                entry.getChangedBy() == null ? null : entry.getChangedBy().getRole(),
                entry.getChangedAt());
    }

    private ApplicationSupportRequestResponse mapSupportRequest(ApplicationSupportRequest request) {
        var application = request.getApplication();
        var student = application.getStudent();
        return new ApplicationSupportRequestResponse(
                request.getId(), application.getId(), student.getId(), student.getFullName(),
                student.getUser().getEmail(), application.getScholarship().getName(),
                request.getRequestType(), request.getStudentMessage(), request.getStatus(),
                request.getReviewerMessage(), request.getRequestedAt(), request.getReviewedAt());
    }

    private void ensureBeforeDeadline(Application application) {
        LocalDate deadline = application.getScholarship().getDeadline();
        if (deadline != null && deadline.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("The scholarship deadline has passed; this request can no longer be opened");
        }
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String cleanProviderReference(String value) {
        String cleaned = clean(value);
        if (cleaned != null && cleaned.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                    "Provider application number contains unsupported characters");
        }
        return cleaned;
    }

    private String displayStatus(String status) {
        return switch (status) {
            case "APPROVED" -> "SELECTED";
            case "REJECTED" -> "NOT SELECTED";
            case "NEEDS_INFORMATION" -> "NEEDS INFORMATION";
            default -> status.replace('_', ' ');
        };
    }
}
