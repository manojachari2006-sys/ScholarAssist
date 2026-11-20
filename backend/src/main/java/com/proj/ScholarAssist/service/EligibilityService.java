package com.proj.ScholarAssist.service;

import com.proj.ScholarAssist.dto.EligibilityResponse;
import com.proj.ScholarAssist.dto.QuickEligibilityRequest;
import com.proj.ScholarAssist.dto.QuickEligibilityResponse;
import com.proj.ScholarAssist.entity.EligibilityResult;
import com.proj.ScholarAssist.entity.Scholarship;
import com.proj.ScholarAssist.entity.ScholarshipRule;
import com.proj.ScholarAssist.entity.StudentProfile;
import com.proj.ScholarAssist.entity.StudentVerifiedFact;
import com.proj.ScholarAssist.exception.InvalidRuleException;
import com.proj.ScholarAssist.exception.ResourceNotFoundException;
import com.proj.ScholarAssist.repository.EligibilityRepository;
import com.proj.ScholarAssist.repository.ScholarshipRepository;
import com.proj.ScholarAssist.repository.ScholarshipRuleRepository;
import com.proj.ScholarAssist.repository.StudentProfileRepository;
import com.proj.ScholarAssist.repository.StudentVerifiedFactRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Evaluates scholarship requirements using only facts confirmed by a reviewer. */
@Service
public class EligibilityService {
    private final StudentProfileRepository students;
    private final ScholarshipRepository scholarships;
    private final ScholarshipRuleRepository rules;
    private final StudentVerifiedFactRepository verifiedFacts;
    private final EligibilityRepository results;

    public EligibilityService(
            StudentProfileRepository students,
            ScholarshipRepository scholarships,
            ScholarshipRuleRepository rules,
            StudentVerifiedFactRepository verifiedFacts,
            EligibilityRepository results) {
        this.students = students;
        this.scholarships = scholarships;
        this.rules = rules;
        this.verifiedFacts = verifiedFacts;
        this.results = results;
    }

    /** Evaluates every scholarship for the authenticated student's profile. */
    @Transactional
    public List<EligibilityResponse> evaluateAll(Long studentId, String email) {
        var student = student(studentId, email);
        Map<String, StudentVerifiedFact> facts = factsFor(studentId);
        return scholarships.findByStatusIgnoreCase("ACTIVE").stream()
                .filter(scholarship -> scholarship.getDeadline() == null
                        || !scholarship.getDeadline().isBefore(LocalDate.now()))
                .map(scholarship -> evaluate(student, scholarship, facts))
                .toList();
    }

    /** Evaluates and stores one scholarship result for the authenticated student. */
    @Transactional
    public EligibilityResponse evaluateOne(Long studentId, Long scholarshipId, String email) {
        var student = student(studentId, email);
        var scholarship = scholarships.findById(scholarshipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Scholarship not found: " + scholarshipId));
        return evaluate(student, scholarship, factsFor(studentId));
    }

    /** Lists saved results only for the authenticated student's own profile. */
    @Transactional(readOnly = true)
    public List<EligibilityResponse> results(Long studentId, String email) {
        student(studentId, email);
        return results.findByStudent_Id(studentId).stream()
                .map(this::map)
                .toList();
    }

    /** Retrieves one saved result after confirming profile ownership. */
    @Transactional(readOnly = true)
    public EligibilityResponse result(Long studentId, Long scholarshipId, String email) {
        student(studentId, email);
        return results.findByStudent_IdAndScholarship_Id(studentId, scholarshipId)
                .map(this::map)
                .orElseThrow(() -> new ResourceNotFoundException("Eligibility result not found"));
    }

    /**
     * Compares student-entered answers with one scholarship's published criteria.
     * This preliminary check is deliberately separate from reviewer-verified eligibility
     * and does not create or update an eligibility result.
     */
    @Transactional(readOnly = true)
    public QuickEligibilityResponse quickCheck(Long scholarshipId, QuickEligibilityRequest request) {
        Scholarship scholarship = scholarships.findById(scholarshipId)
                .filter(value -> "ACTIVE".equalsIgnoreCase(value.getStatus()))
                .filter(value -> value.getDeadline() == null
                        || !value.getDeadline().isBefore(LocalDate.now()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active scholarship not found: " + scholarshipId));

        Map<String, String> answers = normalizeAnswers(request.facts());
        Map<Long, Boolean> confirmations = request.additionalConditions() == null
                ? Map.of()
                : request.additionalConditions();
        List<ScholarshipRule> scholarshipRules = rules.findByScholarship_Id(scholarshipId);
        boolean hasPublishedCriteria = !scholarshipRules.isEmpty()
                || hasText(scholarship.getState())
                || hasText(scholarship.getCategory())
                || hasText(scholarship.getEducationLevel());
        Set<Long> manualRuleIds = scholarshipRules.stream()
                .filter(rule -> "MANUAL_REVIEW".equals(canonicalFactType(rule.getRuleType())))
                .map(ScholarshipRule::getId)
                .collect(java.util.stream.Collectors.toSet());

        if (!manualRuleIds.containsAll(confirmations.keySet())) {
            throw new InvalidRuleException(
                    "Additional-condition answers must reference rules for this scholarship");
        }

        List<String> reasons = new ArrayList<>();
        Set<String> explicitTypes = new HashSet<>();
        boolean missingInformation = false;
        boolean failedRule = false;

        for (ScholarshipRule rule : scholarshipRules) {
            String factType = canonicalFactType(rule.getRuleType());
            explicitTypes.add(factType);

            if ("MANUAL_REVIEW".equals(factType)) {
                Boolean answer = confirmations.get(rule.getId());
                if (answer == null) {
                    missingInformation = true;
                    reasons.add(ruleDescription(rule)
                            + " Please answer this question to complete the quick check.");
                } else if (!answer) {
                    failedRule = true;
                    reasons.add(ruleDescription(rule));
                }
                continue;
            }

            Evaluation evaluation = evaluateAnswer(
                    answers, factType, rule.getRuleValue(), rule.getOperator(),
                    rule.getDescription());
            missingInformation |= evaluation.missing();
            failedRule |= evaluation.failed();
            if (evaluation.reason() != null) {
                reasons.add(evaluation.reason());
            }
        }

        for (var target : List.of(
                evaluateAnswer(answers, "STATE", scholarship.getState(), "=", null,
                        explicitTypes.contains("STATE")),
                evaluateAnswer(answers, "CATEGORY", scholarship.getCategory(), "=", null,
                        explicitTypes.contains("CATEGORY")),
                evaluateAnswer(answers, "EDUCATION_LEVEL", scholarship.getEducationLevel(), "=", null,
                        explicitTypes.contains("EDUCATION_LEVEL")))) {
            missingInformation |= target.missing();
            failedRule |= target.failed();
            if (target.reason() != null) {
                reasons.add(target.reason());
            }
        }

        String status = failedRule
                ? "DOES_NOT_MATCH"
                : missingInformation || !hasPublishedCriteria
                        ? "MORE_INFORMATION_REQUIRED"
                        : "PRELIMINARY_MATCH";
        if (!hasPublishedCriteria) {
            reasons.add("The scholarship has no eligibility criteria listed yet.");
        }
        if (reasons.isEmpty()) {
            reasons.add("Your answers match the published criteria entered for this scholarship.");
        }

        return new QuickEligibilityResponse(
                scholarship.getId(),
                scholarship.getName(),
                status,
                List.copyOf(reasons),
                "This is a preliminary check based on your answers, not document verification or an award decision. The scholarship provider makes the final determination.");
    }

    private Map<String, String> normalizeAnswers(Map<String, String> facts) {
        if (facts == null) {
            return Map.of();
        }
        Map<String, String> normalized = new java.util.HashMap<>();
        facts.forEach((key, value) -> {
            if (key != null && value != null && !value.isBlank()) {
                normalized.put(canonicalFactType(key), value.trim());
            }
        });
        return normalized;
    }

    private Evaluation evaluateAnswer(
            Map<String, String> answers,
            String factType,
            String expected,
            String operator,
            String description) {
        return evaluateAnswer(answers, factType, expected, operator, description, false);
    }

    private Evaluation evaluateAnswer(
            Map<String, String> answers,
            String factType,
            String expected,
            String operator,
            String description,
            boolean alreadyCoveredByExplicitRule) {
        if (expected == null || expected.isBlank() || alreadyCoveredByExplicitRule) {
            return new Evaluation(false, false, null);
        }
        String answer = answers.get(factType);
        if (answer == null) {
            return new Evaluation(true, false,
                    "Please provide your " + displayName(factType) + " to check this requirement.");
        }
        if (isNumericFact(factType)) {
            try {
                new BigDecimal(answer);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "Enter a valid number for " + displayName(factType));
            }
            validateNumericAnswer(factType, answer);
        }
        try {
            if (compare(answer, expected, operator, factType)) {
                return new Evaluation(false, false, null);
            }
        } catch (RuntimeException exception) {
            throw new InvalidRuleException(
                    "Invalid " + factType + " rule: " + expected + " " + operator);
        }
        String reason = description == null || description.isBlank()
                ? displayName(factType) + " does not match the listed requirement."
                : description;
        return new Evaluation(false, true, reason);
    }

    private String ruleDescription(ScholarshipRule rule) {
        return rule.getDescription() == null || rule.getDescription().isBlank()
                ? "Additional scholarship condition"
                : rule.getDescription();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private StudentProfile student(Long id, String email) {
        var student = students.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student profile not found: " + id));
        if (!student.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("This student profile does not belong to the signed-in account");
        }
        return student;
    }

    private Map<String, StudentVerifiedFact> factsFor(Long studentId) {
        return verifiedFacts.findByStudent_IdOrderByVerifiedAtDesc(studentId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        fact -> fact.getFactType().toUpperCase(Locale.ROOT),
                        fact -> fact,
                        (latest, earlier) -> latest));
    }

    private EligibilityResponse evaluate(
            StudentProfile student,
            Scholarship scholarship,
            Map<String, StudentVerifiedFact> facts) {
        List<String> reasons = new ArrayList<>();
        Set<String> explicitTypes = new HashSet<>();
        boolean missingInformation = false;
        boolean failedRule = false;

        for (ScholarshipRule rule : rules.findByScholarship_Id(scholarship.getId())) {
            String factType = canonicalFactType(rule.getRuleType());
            explicitTypes.add(factType);
            Evaluation evaluation = evaluateCriterion(facts, factType,
                    rule.getRuleValue(), rule.getOperator(), rule.getDescription());
            missingInformation |= evaluation.missing();
            failedRule |= evaluation.failed();
            if (evaluation.reason() != null) {
                reasons.add(evaluation.reason());
            }
        }

        // Scholarship-level targeting columns are criteria too; explicit rules take precedence.
        Evaluation stateTarget = evaluateScholarshipTarget(
                facts, "STATE", scholarship.getState(), explicitTypes);
        Evaluation categoryTarget = evaluateScholarshipTarget(
                facts, "CATEGORY", scholarship.getCategory(), explicitTypes);
        Evaluation educationTarget = evaluateScholarshipTarget(
                facts, "EDUCATION_LEVEL", scholarship.getEducationLevel(), explicitTypes);
        for (Evaluation target : List.of(stateTarget, categoryTarget, educationTarget)) {
            missingInformation |= target.missing();
            failedRule |= target.failed();
            if (target.reason() != null) {
                reasons.add(target.reason());
            }
        }

        String status = failedRule
                ? "NOT_ELIGIBLE"
                : missingInformation ? "POSSIBLY_ELIGIBLE" : "ELIGIBLE";
        String reason = reasons.isEmpty()
                ? "All listed scholarship criteria are met using reviewer-confirmed facts"
                : String.join("; ", reasons);

        // The database has one row per student/scholarship pair; update that row on reevaluation.
        var result = results.findByStudent_IdAndScholarship_Id(
                        student.getId(), scholarship.getId())
                .orElseGet(EligibilityResult::new);
        result.setStudent(student);
        result.setScholarship(scholarship);
        result.setStatus(status);
        result.setReason(reason);
        result.setEvaluatedAt(LocalDateTime.now());
        return map(results.save(result));
    }

    private Evaluation evaluateScholarshipTarget(
            Map<String, StudentVerifiedFact> facts,
            String factType,
            String expected,
            Set<String> explicitTypes) {
        if (expected == null || expected.isBlank() || explicitTypes.contains(factType)) {
            return new Evaluation(false, false, null);
        }
        return evaluateCriterion(
                facts, factType, expected, "=", factType + " requirement not met");
    }

    private Evaluation evaluateCriterion(
            Map<String, StudentVerifiedFact> facts,
            String factType,
            String expected,
            String operator,
            String description) {
        if ("MANUAL_REVIEW".equals(factType)) {
            String reviewReason = description == null || description.isBlank()
                    ? "Additional official scholarship conditions need manual confirmation"
                    : description;
            return new Evaluation(
                    true,
                    false,
                    reviewReason + " These conditions are not yet evaluated automatically.");
        }

        StudentVerifiedFact fact = facts.get(factType);
        if (fact == null) {
            return new Evaluation(true, false,
                    "No reviewer-confirmed " + displayName(factType)
                            + " fact; profile values and pending or rejected documents do not count "
                            + "until a reviewer confirms this specific fact");
        }

        boolean passed;
        try {
            passed = compare(fact.getFactValue(), expected, operator, factType);
        } catch (RuntimeException exception) {
            throw new InvalidRuleException(
                    "Invalid " + factType + " rule: " + expected + " " + operator);
        }
        if (!passed) {
            String reason = description == null || description.isBlank()
                    ? displayName(factType) + " requirement not met"
                    : description;
            return new Evaluation(false, true, reason);
        }
        return new Evaluation(false, false, null);
    }

    private String canonicalFactType(String ruleType) {
        String normalized = ruleType.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (normalized) {
            case "INCOME", "ANNUAL_FAMILY_INCOME" -> "ANNUAL_FAMILY_INCOME";
            case "CGPA", "STATE", "CATEGORY", "COURSE", "EDUCATION_LEVEL",
                    "GENDER", "DISABILITY_STATUS", "SEAT_TYPE",
                    "PREVIOUS_MARKS_PERCENTAGE", "PUC_MARKS_PERCENTAGE",
                    "DISABILITY_PERCENTAGE", "HOSTEL_STATUS", "YEAR_OF_STUDY",
                    "MANUAL_REVIEW" -> normalized;
            default -> throw new InvalidRuleException("Unsupported rule type: " + ruleType);
        };
    }

    private String displayName(String factType) {
        return switch (factType) {
            case "ANNUAL_FAMILY_INCOME" -> "annual family income";
            case "PREVIOUS_MARKS_PERCENTAGE" -> "previous exam marks percentage";
            case "PUC_MARKS_PERCENTAGE" -> "PUC marks percentage";
            case "SEAT_TYPE" -> "admission seat type";
            case "DISABILITY_STATUS" -> "disability status";
            case "DISABILITY_PERCENTAGE" -> "disability percentage";
            case "HOSTEL_STATUS" -> "hostel status";
            case "YEAR_OF_STUDY" -> "year of study";
            default -> factType.toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }

    private boolean isNumericFact(String factType) {
        return Set.of(
                "CGPA", "ANNUAL_FAMILY_INCOME", "PREVIOUS_MARKS_PERCENTAGE",
                "PUC_MARKS_PERCENTAGE", "DISABILITY_PERCENTAGE", "YEAR_OF_STUDY")
                .contains(factType);
    }

    private void validateNumericAnswer(String factType, String value) {
        BigDecimal number = new BigDecimal(value);
        if (number.signum() < 0) {
            throw new IllegalArgumentException(displayName(factType) + " cannot be negative");
        }
        if (("PREVIOUS_MARKS_PERCENTAGE".equals(factType)
                || "PUC_MARKS_PERCENTAGE".equals(factType)
                || "DISABILITY_PERCENTAGE".equals(factType))
                && number.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(displayName(factType) + " cannot exceed 100");
        }
        if ("CGPA".equals(factType) && number.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("CGPA cannot exceed 10");
        }
        if ("YEAR_OF_STUDY".equals(factType) && number.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("Year of study must be at least 1");
        }
        if ("YEAR_OF_STUDY".equals(factType)
                && number.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("Year of study cannot exceed 10");
        }
    }

    private boolean compare(String actual, String expected, String operator, String factType) {
        if (!Set.of("=", "!=", ">", "<", ">=", "<=", "IN").contains(operator)) {
            throw new IllegalArgumentException("Unsupported operator");
        }
        if ("IN".equals(operator)) {
            return java.util.Arrays.stream(expected.split(","))
                    .map(String::trim)
                    .anyMatch(candidate -> candidate.equalsIgnoreCase(actual));
        }
        int comparison;
        if (isNumericFact(factType)) {
            comparison = new BigDecimal(actual).compareTo(new BigDecimal(expected));
        } else {
            comparison = actual.compareToIgnoreCase(expected);
        }
        return switch (operator) {
            case "=" -> comparison == 0;
            case "!=" -> comparison != 0;
            case ">" -> comparison > 0;
            case "<" -> comparison < 0;
            case ">=" -> comparison >= 0;
            case "<=" -> comparison <= 0;
            default -> false;
        };
    }

    private EligibilityResponse map(EligibilityResult result) {
        var response = new EligibilityResponse();
        response.setStudentId(result.getStudent().getId());
        response.setScholarshipId(result.getScholarship().getId());
        response.setStatus(result.getStatus());
        response.setReason(result.getReason());
        response.setEvaluatedAt(result.getEvaluatedAt());
        return response;
    }

    private record Evaluation(boolean missing, boolean failed, String reason) {
    }
}
