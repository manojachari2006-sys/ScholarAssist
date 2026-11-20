package com.proj.ScholarAssist.controller;

import com.proj.ScholarAssist.dto.DocumentResponse;
import com.proj.ScholarAssist.service.DocumentService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Student endpoints for uploading and managing their own document records. */
@Validated
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping("/{studentId}")
    public ResponseEntity<DocumentResponse> upload(
            @PathVariable Long studentId,
            @RequestParam @NotBlank String documentType,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return ResponseEntity.status(201).body(
                service.upload(studentId, authentication.getName(), documentType, file));
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<List<DocumentResponse>> byStudent(
            @PathVariable Long studentId,
            Authentication authentication) {
        return ResponseEntity.ok(service.byStudent(studentId, authentication.getName()));
    }

    @GetMapping("/details/{documentId}")
    public ResponseEntity<DocumentResponse> get(
            @PathVariable Long documentId,
            Authentication authentication) {
        return ResponseEntity.ok(service.getForStudent(documentId, authentication.getName()));
    }

    @PutMapping(value = "/{documentId}", consumes = "multipart/form-data")
    public ResponseEntity<DocumentResponse> replace(
            @PathVariable Long documentId,
            @RequestParam @NotBlank String documentType,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return ResponseEntity.ok(service.replace(
                documentId, authentication.getName(), documentType, file));
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long documentId,
            Authentication authentication) {
        service.delete(documentId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/applications/{applicationId}/{documentId}/submitted")
    public ResponseEntity<Void> submitted(
            @PathVariable Long applicationId,
            @PathVariable Long documentId,
            @RequestParam boolean submitted,
            Authentication authentication) {
        service.markSubmitted(applicationId, documentId, submitted, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
