package com.proj.ScholarAssist.controller;

import com.proj.ScholarAssist.dto.DocumentReviewRequest;
import com.proj.ScholarAssist.dto.ReviewerDocumentResponse;
import com.proj.ScholarAssist.dto.VerifiedFactResponse;
import com.proj.ScholarAssist.dto.VerifiedFactsRequest;
import com.proj.ScholarAssist.service.DocumentService;
import com.proj.ScholarAssist.service.VerifiedFactService;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Reviewer-only endpoints for the document verification queue. */
@RestController
@RequestMapping("/api/reviewer/documents")
public class ReviewerDocumentController {
    private final DocumentService service;
    private final VerifiedFactService verifiedFacts;

    public ReviewerDocumentController(
            DocumentService service,
            VerifiedFactService verifiedFacts) {
        this.service = service;
        this.verifiedFacts = verifiedFacts;
    }

    @GetMapping
    public ResponseEntity<List<ReviewerDocumentResponse>> pending() {
        return ResponseEntity.ok(service.pendingReview());
    }

    @GetMapping("/verified")
    public ResponseEntity<List<ReviewerDocumentResponse>> verified() {
        return ResponseEntity.ok(service.verifiedDocuments());
    }

    @GetMapping("/students/{studentId}/documents")
    public ResponseEntity<List<ReviewerDocumentResponse>> studentDocuments(
            @PathVariable Long studentId,
            Authentication authentication) {
        return ResponseEntity.ok(service.reviewerDocumentsForStudent(
                studentId, authentication.getName()));
    }

    @GetMapping("/students/{studentId}/facts")
    public ResponseEntity<List<VerifiedFactResponse>> studentFacts(@PathVariable Long studentId) {
        return ResponseEntity.ok(verifiedFacts.factsForStudent(studentId));
    }

    @PutMapping("/{documentId}/facts")
    public ResponseEntity<List<VerifiedFactResponse>> confirmFacts(
            @PathVariable Long documentId,
            @Valid @RequestBody VerifiedFactsRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(verifiedFacts.confirmFacts(
                documentId, authentication.getName(), request.facts()));
    }

    @GetMapping("/{documentId}/content")
    public ResponseEntity<InputStreamResource> content(@PathVariable Long documentId) {
        var file = service.content(documentId);
        return ResponseEntity.ok()
                .contentType(file.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.fileName()).build().toString())
                .body(file.resource());
    }

    @PatchMapping("/{documentId}/review")
    public ResponseEntity<ReviewerDocumentResponse> review(
            @PathVariable Long documentId,
            @Valid @RequestBody DocumentReviewRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.review(
                documentId, authentication.getName(), request.status(), request.feedback()));
    }
}
