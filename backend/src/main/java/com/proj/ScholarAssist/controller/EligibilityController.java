package com.proj.ScholarAssist.controller;

import com.proj.ScholarAssist.dto.EligibilityResponse;
import com.proj.ScholarAssist.dto.QuickEligibilityRequest;
import com.proj.ScholarAssist.dto.QuickEligibilityResponse;
import com.proj.ScholarAssist.service.EligibilityService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/** HTTP endpoints for evaluating and reading scholarship eligibility. */
@RestController
@RequestMapping("/api/eligibility")
public class EligibilityController {
    private final EligibilityService service;

    public EligibilityController(EligibilityService service) {
        this.service = service;
    }

    /** Evaluates every scholarship for a student and stores the results. */
    @PostMapping("/check/{studentId}")
    public ResponseEntity<List<EligibilityResponse>> check(
            @PathVariable Long studentId,
            Authentication authentication) {
        return ResponseEntity.ok(service.evaluateAll(studentId, authentication.getName()));
    }

    /** Lists the student's previously evaluated eligibility results. */
    @GetMapping("/results/{studentId}")
    public ResponseEntity<List<EligibilityResponse>> results(
            @PathVariable Long studentId,
            Authentication authentication) {
        return ResponseEntity.ok(service.results(studentId, authentication.getName()));
    }

    /** Retrieves one saved student and scholarship result. */
    @GetMapping("/{studentId}/{scholarshipId}")
    public ResponseEntity<EligibilityResponse> result(
            @PathVariable Long studentId,
            @PathVariable Long scholarshipId,
            Authentication authentication) {
        return ResponseEntity.ok(service.result(
                studentId, scholarshipId, authentication.getName()));
    }

    /** Checks one scholarship using temporary, self-reported answers without persisting them. */
    @PostMapping("/quick-check/{scholarshipId}")
    public ResponseEntity<QuickEligibilityResponse> quickCheck(
            @PathVariable Long scholarshipId,
            @Valid @RequestBody QuickEligibilityRequest request) {
        return ResponseEntity.ok(service.quickCheck(scholarshipId, request));
    }
}
