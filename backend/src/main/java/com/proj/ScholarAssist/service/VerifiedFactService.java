package com.proj.ScholarAssist.service;

import com.proj.ScholarAssist.dto.VerifiedFactRequest;
import com.proj.ScholarAssist.dto.VerifiedFactResponse;
import com.proj.ScholarAssist.entity.StudentVerifiedFact;
import com.proj.ScholarAssist.exception.InvalidDocumentStateException;
import com.proj.ScholarAssist.exception.ResourceNotFoundException;
import com.proj.ScholarAssist.repository.DocumentRepository;
import com.proj.ScholarAssist.repository.StudentVerifiedFactRepository;
import com.proj.ScholarAssist.repository.StudentProfileRepository;
import com.proj.ScholarAssist.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Stores explicitly reviewed student facts and their document/reviewer audit trail. */
@Service
public class VerifiedFactService {
    private static final Map<String, String> FACT_TYPES = Map.ofEntries(
            Map.entry("CGPA", "CGPA"),
            Map.entry("ANNUAL_FAMILY_INCOME", "ANNUAL_FAMILY_INCOME"),
            Map.entry("INCOME", "ANNUAL_FAMILY_INCOME"),
            Map.entry("STATE", "STATE"),
            Map.entry("CATEGORY", "CATEGORY"),
            Map.entry("COURSE", "COURSE"),
            Map.entry("EDUCATION_LEVEL", "EDUCATION_LEVEL"),
            Map.entry("GENDER", "GENDER"),
            Map.entry("DISABILITY_STATUS", "DISABILITY_STATUS"),
            Map.entry("DISABILITY_PERCENTAGE", "DISABILITY_PERCENTAGE"),
            Map.entry("SEAT_TYPE", "SEAT_TYPE"),
            Map.entry("PREVIOUS_MARKS_PERCENTAGE", "PREVIOUS_MARKS_PERCENTAGE"),
            Map.entry("PUC_MARKS_PERCENTAGE", "PUC_MARKS_PERCENTAGE"),
            Map.entry("HOSTEL_STATUS", "HOSTEL_STATUS"),
            Map.entry("YEAR_OF_STUDY", "YEAR_OF_STUDY"));

    private final DocumentRepository documents;
    private final UserRepository users;
    private final StudentVerifiedFactRepository facts;
    private final StudentProfileRepository students;

    public VerifiedFactService(
            DocumentRepository documents,
            UserRepository users,
            StudentVerifiedFactRepository facts,
            StudentProfileRepository students) {
        this.documents = documents;
        this.users = users;
        this.facts = facts;
        this.students = students;
    }

    /** Records one or more current facts from a document already marked VERIFIED. */
    @Transactional
    public List<VerifiedFactResponse> confirmFacts(
            Long documentId,
            String reviewerEmail,
            List<VerifiedFactRequest> requests) {
        var document = documents.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        if (!"VERIFIED".equals(document.getVerificationStatus())) {
            throw new InvalidDocumentStateException(
                    "Student facts can only be confirmed from a VERIFIED document");
        }

        var reviewer = users.findByEmailIgnoreCase(reviewerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer account not found"));
        if (!"REVIEWER".equals(reviewer.getRole()) && !"ADMIN".equals(reviewer.getRole())) {
            throw new AccessDeniedException("Reviewer role is required for this operation");
        }

        return requests.stream()
                .map(request -> saveFact(document, reviewer, request))
                .toList();
    }

    /** Returns the active auditable facts for reviewer inspection. */
    @Transactional(readOnly = true)
    public List<VerifiedFactResponse> factsForStudent(Long studentId) {
        if (!students.existsById(studentId)) {
            throw new ResourceNotFoundException("Student profile not found: " + studentId);
        }
        return facts.findByStudent_IdOrderByVerifiedAtDesc(studentId).stream()
                .map(this::map)
                .toList();
    }

    private VerifiedFactResponse saveFact(
            com.proj.ScholarAssist.entity.Document document,
            com.proj.ScholarAssist.entity.User reviewer,
            VerifiedFactRequest request) {
        String suppliedType = request.factType().trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_');
        String factType = FACT_TYPES.get(suppliedType);
        if (factType == null) {
            throw new IllegalArgumentException(
                    "Unsupported factType. Use CGPA, ANNUAL_FAMILY_INCOME, STATE, CATEGORY, COURSE, EDUCATION_LEVEL, GENDER, DISABILITY_STATUS, DISABILITY_PERCENTAGE, SEAT_TYPE, PREVIOUS_MARKS_PERCENTAGE, PUC_MARKS_PERCENTAGE, HOSTEL_STATUS, or YEAR_OF_STUDY");
        }

        String value = request.value().trim();
        validateFactValue(factType, value);
        var fact = new StudentVerifiedFact();
        fact.setStudent(document.getStudent());
        fact.setFactType(factType);
        fact.setFactValue(value);
        fact.setSourceDocument(document);
        fact.setReviewer(reviewer);
        fact.setVerifiedAt(LocalDateTime.now());
        return map(facts.save(fact));
    }

    private void validateFactValue(String factType, String value) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("Verified fact value cannot be blank");
        }
        if (List.of(
                "CGPA", "ANNUAL_FAMILY_INCOME", "PREVIOUS_MARKS_PERCENTAGE",
                "PUC_MARKS_PERCENTAGE", "DISABILITY_PERCENTAGE", "YEAR_OF_STUDY")
                .contains(factType)) {
            BigDecimal numericValue;
            try {
                numericValue = new BigDecimal(value);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(factType + " must be a valid number");
            }
            if (numericValue.signum() < 0) {
                throw new IllegalArgumentException(factType + " cannot be negative");
            }
            if ("CGPA".equals(factType) && numericValue.compareTo(BigDecimal.TEN) > 0) {
                throw new IllegalArgumentException("CGPA cannot exceed 10");
            }
            if (("PREVIOUS_MARKS_PERCENTAGE".equals(factType)
                    || "PUC_MARKS_PERCENTAGE".equals(factType)
                    || "DISABILITY_PERCENTAGE".equals(factType))
                    && numericValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException(factType + " cannot exceed 100");
            }
            if ("YEAR_OF_STUDY".equals(factType)
                    && numericValue.compareTo(BigDecimal.ONE) < 0) {
                throw new IllegalArgumentException("YEAR_OF_STUDY must be at least 1");
            }
            if ("YEAR_OF_STUDY".equals(factType)
                    && numericValue.compareTo(BigDecimal.TEN) > 0) {
                throw new IllegalArgumentException("YEAR_OF_STUDY cannot exceed 10");
            }
        }
    }

    private VerifiedFactResponse map(StudentVerifiedFact fact) {
        return new VerifiedFactResponse(
                fact.getId(),
                fact.getStudent().getId(),
                fact.getFactType(),
                fact.getFactValue(),
                fact.getSourceDocument().getId(),
                fact.getSourceDocument().getFileName(),
                fact.getReviewer().getId(),
                fact.getVerifiedAt());
    }
}
