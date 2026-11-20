package com.proj.ScholarAssist.service;

import com.proj.ScholarAssist.dto.DocumentResponse;
import com.proj.ScholarAssist.dto.ReviewerDocumentResponse;
import com.proj.ScholarAssist.entity.Document;
import com.proj.ScholarAssist.entity.User;
import com.proj.ScholarAssist.exception.FileStorageException;
import com.proj.ScholarAssist.exception.InvalidDocumentStateException;
import com.proj.ScholarAssist.exception.ResourceNotFoundException;
import com.proj.ScholarAssist.repository.ApplicationDocumentRepository;
import com.proj.ScholarAssist.repository.DocumentRepository;
import com.proj.ScholarAssist.repository.StudentProfileRepository;
import com.proj.ScholarAssist.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Owns local file storage, student document actions, and reviewer decisions. */
@Service
public class DocumentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentService.class);
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png");

    private final DocumentRepository documents;
    private final StudentProfileRepository students;
    private final UserRepository users;
    private final ApplicationService applications;
    private final ApplicationDocumentRepository applicationDocuments;
    private final NotificationService notifications;
    private final Path uploadDirectory;
    private final long maximumUploadBytes;

    public DocumentService(
            DocumentRepository documents,
            StudentProfileRepository students,
            UserRepository users,
            ApplicationService applications,
            ApplicationDocumentRepository applicationDocuments,
            NotificationService notifications,
            @Value("${scholarassist.upload-dir:uploads}") String uploadDirectory,
            @Value("${scholarassist.max-upload-bytes:10485760}") long maximumUploadBytes) {
        this.documents = documents;
        this.students = students;
        this.users = users;
        this.applications = applications;
        this.applicationDocuments = applicationDocuments;
        this.notifications = notifications;
        this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        this.maximumUploadBytes = maximumUploadBytes;
    }

    /** Stores a validated document for the authenticated student's own profile. */
    @Transactional
    public DocumentResponse upload(Long studentId, String email, String documentType, MultipartFile file) {
        var student = requireOwnedStudent(studentId, email);
        validateUpload(documentType, file);
        StoredFile storedFile = store(file);

        var document = new Document();
        document.setStudent(student);
        document.setDocumentType(documentType.trim());
        document.setFileName(storedFile.fileName());
        document.setFilePath(storedFile.path().toString());
        document.setVerificationStatus("PENDING");
        document.setUploadedAt(LocalDateTime.now());
        try {
            return map(documents.saveAndFlush(document));
        } catch (RuntimeException exception) {
            deleteQuietly(storedFile.path());
            throw exception;
        }
    }

    /** Replaces a student's unlinked pending or rejected document. */
    @Transactional
    public DocumentResponse replace(Long id, String email, String documentType, MultipartFile file) {
        Document existing = require(id);
        requireOwner(existing, email);
        requireReplaceable(existing);
        validateUpload(documentType, file);

        StoredFile replacement = store(file);
        Path oldPath = resolveStoredPath(existing.getFilePath());
        existing.setDocumentType(documentType.trim());
        existing.setFileName(replacement.fileName());
        existing.setFilePath(replacement.path().toString());
        existing.setUploadedAt(LocalDateTime.now());
        existing.setVerificationStatus("PENDING");
        existing.setReviewedAt(null);
        existing.setReviewer(null);
        existing.setReviewFeedback(null);
        try {
            DocumentResponse response = map(documents.saveAndFlush(existing));
            afterCommit(() -> deleteQuietly(oldPath));
            return response;
        } catch (RuntimeException exception) {
            deleteQuietly(replacement.path());
            throw exception;
        }
    }

    /** Lists only the authenticated student's documents. */
    @Transactional(readOnly = true)
    public List<DocumentResponse> byStudent(Long studentId, String email) {
        requireOwnedStudent(studentId, email);
        return documents.findByStudent_Id(studentId).stream().map(this::map).toList();
    }

    /** Returns document metadata only to its owner. */
    @Transactional(readOnly = true)
    public DocumentResponse getForStudent(Long id, String email) {
        Document document = require(id);
        requireOwner(document, email);
        return map(document);
    }

    /** Deletes an unlinked document while its state still allows student changes. */
    @Transactional
    public void delete(Long id, String email) {
        Document document = require(id);
        requireOwner(document, email);
        requireReplaceable(document);
        Path filePath = resolveStoredPath(document.getFilePath());
        documents.delete(document);
        afterCommit(() -> deleteQuietly(filePath));
    }

    /** Lists documents awaiting a reviewer decision. */
    @Transactional(readOnly = true)
    public List<ReviewerDocumentResponse> pendingReview() {
        return documents.findByVerificationStatusOrderByUploadedAtAsc("PENDING")
                .stream().map(this::mapForReviewer).toList();
    }

    /** Lists verified uploads so reviewers can confirm specific facts from their contents. */
    @Transactional(readOnly = true)
    public List<ReviewerDocumentResponse> verifiedDocuments() {
        return documents.findByVerificationStatusOrderByReviewedAtDesc("VERIFIED").stream()
                .map(this::mapForReviewer)
                .toList();
    }

    /** Lists every document for one student, without storage paths, for an authorized reviewer. */
    @Transactional(readOnly = true)
    public List<ReviewerDocumentResponse> reviewerDocumentsForStudent(
            Long studentId, String reviewerEmail) {
        requireReviewer(reviewerEmail);
        if (!students.existsById(studentId)) {
            throw new ResourceNotFoundException("Student profile not found: " + studentId);
        }
        return documents.findByStudent_Id(studentId).stream()
                .map(this::mapForReviewer)
                .toList();
    }

    /** Reads validated document contents for an authorized reviewer. */
    @Transactional(readOnly = true)
    public ReviewedFile content(Long id) {
        Document document = require(id);
        try {
            Path path = resolveStoredPath(document.getFilePath()).toRealPath();
            if (!path.startsWith(uploadDirectory.toRealPath()) || !Files.isRegularFile(path)) {
                throw new FileStorageException("Stored document file is unavailable");
            }
            byte[] signature;
            try (InputStream input = Files.newInputStream(path)) {
                signature = input.readNBytes(8);
            }
            MediaType mediaType = mediaTypeForSignature(signature);
            return new ReviewedFile(document.getFileName(), mediaType,
                    new InputStreamResource(Files.newInputStream(path)));
        } catch (IOException exception) {
            throw new FileStorageException("Stored document file is unavailable", exception);
        }
    }

    /** Records one final review decision, reviewer identity, time, and feedback. */
    @Transactional
    public ReviewerDocumentResponse review(Long id, String reviewerEmail, String status, String feedback) {
        Document document = require(id);
        if (!"PENDING".equals(document.getVerificationStatus())) {
            throw new InvalidDocumentStateException("Only pending documents can be reviewed");
        }
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        if (!Set.of("VERIFIED", "REJECTED").contains(normalizedStatus)) {
            throw new IllegalArgumentException("Status must be VERIFIED or REJECTED");
        }
        String normalizedFeedback = feedback == null ? null : feedback.trim();
        if ("REJECTED".equals(normalizedStatus)
                && (normalizedFeedback == null || normalizedFeedback.isBlank())) {
            throw new IllegalArgumentException("Feedback is required when rejecting a document");
        }
        User reviewer = requireReviewer(reviewerEmail);
        document.setVerificationStatus(normalizedStatus);
        document.setReviewFeedback(normalizedFeedback);
        document.setReviewedAt(LocalDateTime.now());
        document.setReviewer(reviewer);
        Document savedDocument = documents.save(document);
        notifications.create(
                document.getStudent().getUser(),
                "Document review update",
                "Your " + document.getDocumentType().replace('_', ' ').toLowerCase()
                        + " document was " + normalizedStatus.toLowerCase() + "."
                        + (normalizedFeedback == null || normalizedFeedback.isBlank()
                        ? "" : " Reviewer feedback: " + normalizedFeedback),
                "DOCUMENT");
        return mapForReviewer(savedDocument);
    }

    private User requireReviewer(String reviewerEmail) {
        User reviewer = users.findByEmailIgnoreCase(reviewerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer account not found"));
        if (!("REVIEWER".equals(reviewer.getRole()) || "ADMIN".equals(reviewer.getRole()))) {
            throw new AccessDeniedException("Reviewer or admin role is required");
        }
        return reviewer;
    }

    /** Associates an existing document with an application. */
    @Transactional
    public void associate(Long applicationId, Long documentId, Boolean required) {
        applications.associateDocument(applicationId, documentId, required);
    }

    /** Updates the submitted state of an associated application document. */
    @Transactional
    public void markSubmitted(Long applicationId, Long documentId, boolean submitted, String email) {
        Document document = require(documentId);
        requireOwner(document, email);
        applications.markDocumentSubmitted(applicationId, documentId, submitted);
    }

    private void requireReplaceable(Document document) {
        if (!Set.of("PENDING", "REJECTED").contains(document.getVerificationStatus())) {
            throw new InvalidDocumentStateException(
                    "Only pending or rejected documents can be deleted or replaced");
        }
        if (applicationDocuments.existsByDocument_Id(document.getId())) {
            throw new InvalidDocumentStateException(
                    "A document linked to an application cannot be deleted or replaced");
        }
    }

    private com.proj.ScholarAssist.entity.StudentProfile requireOwnedStudent(Long studentId, String email) {
        var student = students.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found: " + studentId));
        if (!student.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("This student profile does not belong to the signed-in account");
        }
        return student;
    }

    private void requireOwner(Document document, String email) {
        if (!document.getStudent().getUser().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("This document does not belong to the signed-in account");
        }
    }

    private StoredFile store(MultipartFile file) {
        String originalName = Paths.get(Objects.requireNonNullElse(file.getOriginalFilename(), "document"))
                .getFileName().toString();
        String safeName = originalName.replaceAll("[^A-Za-z0-9._-]", "_");
        Path target = uploadDirectory.resolve(UUID.randomUUID() + "-" + safeName).normalize();
        if (!target.startsWith(uploadDirectory)) throw new FileStorageException("Invalid file name");
        try {
            Files.createDirectories(uploadDirectory);
            file.transferTo(target);
            return new StoredFile(target, originalName);
        } catch (IOException exception) {
            throw new FileStorageException("Could not store the uploaded file", exception);
        }
    }

    private void validateUpload(String type, MultipartFile file) {
        if (type == null || type.isBlank() || type.length() > 100) {
            throw new FileStorageException("Document type is required and must be at most 100 characters");
        }
        if (file == null || file.isEmpty()) throw new FileStorageException("A non-empty document file is required");
        if (file.getSize() > maximumUploadBytes) throw new FileStorageException("File exceeds the configured upload size");
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new FileStorageException("Only PDF, JPEG, and PNG files are allowed");
        }
        try (InputStream input = file.getInputStream()) {
            if (mediaTypeForSignature(input.readNBytes(8)).toString().equals(file.getContentType())) return;
            throw new FileStorageException("The uploaded file content does not match its declared type");
        } catch (IOException exception) {
            throw new FileStorageException("Could not inspect the uploaded file", exception);
        }
    }

    private MediaType mediaTypeForSignature(byte[] signature) {
        if (startsWith(signature, new byte[] {'%', 'P', 'D', 'F', '-'})) return MediaType.APPLICATION_PDF;
        if (startsWith(signature, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) return MediaType.IMAGE_JPEG;
        if (startsWith(signature, new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10})) return MediaType.IMAGE_PNG;
        throw new FileStorageException("The file content is not a supported PDF, JPEG, or PNG");
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if (bytes[i] != prefix[i]) return false;
        return true;
    }

    private Path resolveStoredPath(String path) {
        Path resolved = Paths.get(path).toAbsolutePath().normalize();
        if (!resolved.startsWith(uploadDirectory)) throw new FileStorageException("Stored document file is unavailable");
        return resolved;
    }

    private void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { action.run(); }
        });
    }

    private void deleteQuietly(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException exception) { LOGGER.warn("Could not remove document file", exception); }
    }

    private Document require(Long id) {
        return documents.findById(id).orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
    }

    private DocumentResponse map(Document document) {
        var response = new DocumentResponse();
        response.setId(document.getId());
        response.setDocumentType(document.getDocumentType());
        response.setFileName(document.getFileName());
        response.setVerificationStatus(document.getVerificationStatus());
        response.setUploadedAt(document.getUploadedAt());
        response.setReviewedAt(document.getReviewedAt());
        response.setReviewFeedback(document.getReviewFeedback());
        return response;
    }

    private ReviewerDocumentResponse mapForReviewer(Document document) {
        return new ReviewerDocumentResponse(
                document.getId(), document.getStudent().getId(), document.getStudent().getFullName(),
                document.getDocumentType(), document.getFileName(), document.getVerificationStatus(),
                document.getUploadedAt(), document.getReviewedAt(), document.getReviewFeedback());
    }

    private record StoredFile(Path path, String fileName) { }

    public record ReviewedFile(String fileName, MediaType mediaType, InputStreamResource resource) { }
}
