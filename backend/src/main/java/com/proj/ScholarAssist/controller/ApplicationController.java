package com.proj.ScholarAssist.controller;

import com.proj.ScholarAssist.dto.ApplicationDocumentRequest;
import com.proj.ScholarAssist.dto.ApplicationRequest;
import com.proj.ScholarAssist.dto.ApplicationResponse;
import com.proj.ScholarAssist.dto.ApplicationStatusRequest;
import com.proj.ScholarAssist.dto.ApplicationUpdateRequest;
import com.proj.ScholarAssist.dto.ExternalApplicationRequest;
import com.proj.ScholarAssist.dto.ExternalApplicationSubmissionRequest;
import com.proj.ScholarAssist.dto.ApplicationStatusHistoryResponse;
import com.proj.ScholarAssist.dto.ApplicationSupportRequestCreate;
import com.proj.ScholarAssist.dto.ApplicationSupportRequestResponse;
import com.proj.ScholarAssist.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import java.util.List;

/** HTTP endpoints for student applications and their supporting documents. */
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    /** Creates a draft application for the requested student. */
    @PostMapping("/{studentId}")
    public ResponseEntity<ApplicationResponse> create(
            @PathVariable Long studentId,
            @Valid
            @RequestBody ApplicationRequest request,
            Authentication authentication) {
        return ResponseEntity.status(201)
                .body(service.create(studentId, request, authentication.getName()));
    }

    /** Starts a local tracker record before the student visits the provider's site. */
    @PostMapping("/{studentId}/external")
    public ResponseEntity<ApplicationResponse> startExternal(
            @PathVariable Long studentId,
            @Valid @RequestBody ExternalApplicationRequest request,
            Authentication authentication) {
        return ResponseEntity.status(201).body(service.startExternalApplication(
                studentId, request, authentication.getName()));
    }

    /** Records that the student says they submitted on the external provider site. */
    @PatchMapping("/{applicationId}/external-submission")
    public ResponseEntity<ApplicationResponse> confirmExternalSubmission(
            @PathVariable Long applicationId,
            @Valid @RequestBody ExternalApplicationSubmissionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.confirmExternalSubmission(
                applicationId, request, authentication.getName()));
    }

    /** Lists the status timeline for the owning student or an authorized reviewer. */
    @GetMapping("/{applicationId}/history")
    public ResponseEntity<List<ApplicationStatusHistoryResponse>> history(
            @PathVariable Long applicationId,
            Authentication authentication) {
        return ResponseEntity.ok(service.history(applicationId, authentication.getName()));
    }

    @PostMapping("/{applicationId}/support-requests")
    public ResponseEntity<ApplicationSupportRequestResponse> createSupportRequest(
            @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationSupportRequestCreate request,
            Authentication authentication) {
        return ResponseEntity.status(201).body(service.createSupportRequest(
                applicationId, request.requestType(), request.message(), authentication.getName()));
    }

    @GetMapping("/{applicationId}/support-requests")
    public ResponseEntity<List<ApplicationSupportRequestResponse>> supportRequests(
            @PathVariable Long applicationId, Authentication authentication) {
        return ResponseEntity.ok(service.supportRequestsForApplication(
                applicationId, authentication.getName()));
    }

    /** Lists applications belonging to a student. */
    @GetMapping("/{studentId}")
    public ResponseEntity<List<ApplicationResponse>> byStudent(
            @PathVariable Long studentId,
            Authentication authentication) {
        return ResponseEntity.ok(service.byStudent(studentId, authentication.getName()));
    }

    /** Retrieves application details. */
    @GetMapping("/details/{applicationId}")
    public ResponseEntity<ApplicationResponse> get(
            @PathVariable Long applicationId,
            Authentication authentication) {
        return ResponseEntity.ok(service.get(applicationId, authentication.getName()));
    }

    /** Updates remarks on a draft application. */
    @PutMapping("/{applicationId}")
    public ResponseEntity<ApplicationResponse> update(
            @PathVariable Long applicationId,
            @RequestBody ApplicationUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.updateRemarks(
                applicationId, request.remarks(), authentication.getName()));
    }

    /** Submits a draft application after the service checks its required documents. */
    @PatchMapping("/{applicationId}/submit")
    public ResponseEntity<ApplicationResponse> submit(
            @PathVariable Long applicationId,
            Authentication authentication) {
        return ResponseEntity.ok(service.submit(applicationId, authentication.getName()));
    }

    /** Applies a supported status transition, such as review, approval, or rejection. */
    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationStatusRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.setStatus(
                applicationId, request.status(), request.remarks(), authentication.getName()));
    }

    /** Associates an existing student document with the application. */
    @PostMapping("/{applicationId}/documents")
    public ResponseEntity<Void> addDocument(
            @PathVariable Long applicationId,
            @Valid
            @RequestBody ApplicationDocumentRequest request,
            Authentication authentication) {
        service.associateDocumentForUser(
                applicationId,
                request.getDocumentId(),
                request.getRequired(),
                authentication.getName());
        return ResponseEntity.status(201).build();
    }
}
