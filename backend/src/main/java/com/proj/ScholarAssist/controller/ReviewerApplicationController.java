package com.proj.ScholarAssist.controller;

import com.proj.ScholarAssist.dto.ReviewerApplicationResponse;
import com.proj.ScholarAssist.dto.ApplicationSupportRequestDecision;
import com.proj.ScholarAssist.dto.ApplicationSupportRequestResponse;
import com.proj.ScholarAssist.service.ApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Reviewer/admin endpoints for monitoring applications awaiting a decision. */
@RestController
@RequestMapping("/api/reviewer/applications")
public class ReviewerApplicationController {
    private final ApplicationService applications;

    public ReviewerApplicationController(ApplicationService applications) {
        this.applications = applications;
    }

    /** Returns submitted applications that still need reviewer action. */
    @GetMapping
    public ResponseEntity<List<ReviewerApplicationResponse>> queue(
            Authentication authentication) {
        return ResponseEntity.ok(applications.reviewQueue(authentication.getName()));
    }

    /** Returns one request for its reviewer detail view. */
    @GetMapping("/{applicationId}")
    public ResponseEntity<ReviewerApplicationResponse> details(
            @PathVariable Long applicationId,
            Authentication authentication) {
        return ResponseEntity.ok(applications.reviewerDetails(
                applicationId, authentication.getName()));
    }

    @GetMapping("/support-requests")
    public ResponseEntity<List<ApplicationSupportRequestResponse>> supportRequests(
            Authentication authentication) {
        return ResponseEntity.ok(applications.pendingSupportRequests(authentication.getName()));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/support-requests/{requestId}")
    public ResponseEntity<ApplicationSupportRequestResponse> decideSupportRequest(
            @PathVariable Long requestId,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
                    ApplicationSupportRequestDecision decision,
            Authentication authentication) {
        return ResponseEntity.ok(applications.decideSupportRequest(
                requestId, decision.decision(), decision.message(), authentication.getName()));
    }
}
