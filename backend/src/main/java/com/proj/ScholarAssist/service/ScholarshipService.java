package com.proj.ScholarAssist.service;

import com.proj.ScholarAssist.dto.ScholarshipRequest;
import com.proj.ScholarAssist.dto.ScholarshipResponse;
import com.proj.ScholarAssist.dto.ScholarshipRuleRequest;
import com.proj.ScholarAssist.dto.ScholarshipRuleResponse;
import com.proj.ScholarAssist.entity.Scholarship;
import com.proj.ScholarAssist.entity.ScholarshipRule;
import com.proj.ScholarAssist.exception.InvalidRuleException;
import com.proj.ScholarAssist.exception.ResourceNotFoundException;
import com.proj.ScholarAssist.repository.ScholarshipRepository;
import com.proj.ScholarAssist.repository.ScholarshipRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Locale;

/** Searches scholarships and manages their optional eligibility rules. */
@Service
public class ScholarshipService {
    private static final List<String> SUPPORTED_RULE_TYPES = List.of(
            "CGPA",
            "ANNUAL_FAMILY_INCOME",
            "INCOME",
            "STATE",
            "CATEGORY",
            "EDUCATION_LEVEL",
            "COURSE",
            "GENDER",
            "DISABILITY_STATUS",
            "DISABILITY_PERCENTAGE",
            "SEAT_TYPE",
            "PREVIOUS_MARKS_PERCENTAGE",
            "PUC_MARKS_PERCENTAGE",
            "HOSTEL_STATUS",
            "YEAR_OF_STUDY",
            // Keeps decisions conservative for official criteria not represented as verified facts yet.
            "MANUAL_REVIEW");
    private static final List<String> SUPPORTED_OPERATORS =
            List.of("=", "!=", ">", "<", ">=", "<=", "IN");

    private final ScholarshipRepository scholarships;
    private final ScholarshipRuleRepository rules;

    public ScholarshipService(
            ScholarshipRepository scholarships,
            ScholarshipRuleRepository rules) {
        this.scholarships = scholarships;
        this.rules = rules;
    }

    /** Returns scholarships matching the supplied filters. */
    @Transactional(readOnly = true)
    public List<ScholarshipResponse> all(String state, String category, String educationLevel) {
        Map<Long, List<com.proj.ScholarAssist.entity.ScholarshipRule>> educationRules =
                educationRulesIfFiltered(educationLevel);
        return scholarships.findByStatusIgnoreCase("ACTIVE").stream()
                .filter(this::isOpen)
                .filter(scholarship -> matches(state, scholarship.getState()))
                .filter(scholarship -> matches(category, scholarship.getCategory()))
                .filter(scholarship -> matchesEducationLevel(
                        educationLevel, scholarship, educationRules.get(scholarship.getId())))
                .map(this::map)
                .toList();
    }

    /** Retrieves one scholarship by ID. */
    @Transactional(readOnly = true)
    public ScholarshipResponse get(Long id) {
        var scholarship = require(id);
        if (!"ACTIVE".equalsIgnoreCase(scholarship.getStatus()) || !isOpen(scholarship)) {
            throw new ResourceNotFoundException("Scholarship is not currently available: " + id);
        }
        return map(scholarship);
    }

    /** Returns the eligibility rules associated with one scholarship. */
    @Transactional(readOnly = true)
    public List<ScholarshipRuleResponse> getRules(Long scholarshipId) {
        require(scholarshipId);
        return rules.findByScholarship_Id(scholarshipId).stream()
                .map(this::mapRule)
                .toList();
    }

    /** Replaces all eligibility rules for an existing scholarship. */
    @Transactional
    public List<ScholarshipRuleResponse> replaceRules(
            Long scholarshipId,
            List<ScholarshipRuleRequest> requestedRules) {
        var scholarship = require(scholarshipId);
        saveRules(scholarship, requestedRules);
        return rules.findByScholarship_Id(scholarshipId).stream()
                .map(this::mapRule)
                .toList();
    }

    /** Searches name/provider text and applies any supplied filters. */
    @Transactional(readOnly = true)
    public List<ScholarshipResponse> search(
            String query,
            String state,
            String category,
            String educationLevel) {
        Map<Long, List<com.proj.ScholarAssist.entity.ScholarshipRule>> educationRules =
                educationRulesIfFiltered(educationLevel);
        var matches = query == null || query.isBlank()
                ? scholarships.findByStatusIgnoreCase("ACTIVE")
                : scholarships.findByNameContainingIgnoreCaseOrProviderContainingIgnoreCase(
                        query, query);

        return matches.stream()
                .filter(scholarship -> "ACTIVE".equalsIgnoreCase(scholarship.getStatus()))
                .filter(this::isOpen)
                .filter(scholarship -> matches(state, scholarship.getState()))
                .filter(scholarship -> matches(category, scholarship.getCategory()))
                .filter(scholarship -> matchesEducationLevel(
                        educationLevel, scholarship, educationRules.get(scholarship.getId())))
                .map(this::map)
                .toList();
    }

    /** Creates a scholarship and its optional rules in one transaction. */
    @Transactional
    public ScholarshipResponse create(ScholarshipRequest request) {
        var scholarship = new Scholarship();
        apply(scholarship, request);
        scholarship.setCreatedAt(LocalDateTime.now());
        scholarship.setUpdatedAt(LocalDateTime.now());
        scholarship = scholarships.save(scholarship);
        saveRules(scholarship, request.getRules());
        return map(scholarship);
    }

    /** Updates a scholarship and replaces rules only when a rule list is supplied. */
    @Transactional
    public ScholarshipResponse update(Long id, ScholarshipRequest request) {
        var scholarship = require(id);
        apply(scholarship, request);
        scholarship.setUpdatedAt(LocalDateTime.now());
        saveRules(scholarship, request.getRules());
        return map(scholarship);
    }

    /** Deletes a scholarship by ID. */
    @Transactional
    public void delete(Long id) {
        scholarships.delete(require(id));
    }

    private Scholarship require(Long id) {
        return scholarships.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scholarship not found: " + id));
    }

    private boolean matches(String filter, String value) {
        return filter == null
                || filter.isBlank()
                || filter.equalsIgnoreCase(value == null ? "" : value);
    }

    /** Uses both the catalog display column and published education eligibility rules for filtering. */
    private boolean matchesEducationLevel(
            String filter,
            Scholarship scholarship,
            List<com.proj.ScholarAssist.entity.ScholarshipRule> scholarshipRules) {
        if (filter == null || filter.isBlank()) return true;
        String selected = canonicalEducation(filter);
        if (scholarship.getEducationLevel() != null
                && educationValuesMatch(scholarship.getEducationLevel(), selected)) {
            return true;
        }
        if (scholarshipRules == null) return false;
        return scholarshipRules.stream()
                .filter(rule -> "EDUCATION_LEVEL".equalsIgnoreCase(rule.getRuleType()))
                .filter(rule -> Set.of("=", "IN").contains(
                        rule.getOperator() == null ? "" : rule.getOperator().trim().toUpperCase()))
                .anyMatch(rule -> educationValuesMatch(rule.getRuleValue(), selected));
    }

    private Map<Long, List<com.proj.ScholarAssist.entity.ScholarshipRule>> educationRulesIfFiltered(
            String educationLevel) {
        if (educationLevel == null || educationLevel.isBlank()) return Map.of();
        return rules.findAll().stream()
                .filter(rule -> "EDUCATION_LEVEL".equalsIgnoreCase(rule.getRuleType()))
                .collect(Collectors.groupingBy(rule -> rule.getScholarship().getId()));
    }

    private boolean educationValuesMatch(String values, String selected) {
        if (values == null || values.isBlank()) return false;
        return java.util.Arrays.stream(values.split(","))
                .map(this::canonicalEducation)
                .anyMatch(value -> value.equals(selected)
                        || ("SCHOOL".equals(value) && schoolLevels().contains(selected)));
    }

    private String canonicalEducation(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", "");
        return switch (normalized) {
            case "UG", "UNDERGRAD", "UNDERGRADUATE" -> "UNDERGRADUATE";
            case "PG", "POSTGRAD", "POSTGRADUATE" -> "POSTGRADUATE";
            case "PHD", "DOCTORATE", "DOCTORAL", "DOCTORAL_PHD" -> "DOCTORAL";
            case "PRIMARY", "PRIMARY_SCHOOL" -> "PRIMARY_SCHOOL";
            case "MIDDLE", "MIDDLE_SCHOOL", "UPPER_PRIMARY", "UPPER_PRIMARY_MIDDLE_SCHOOL" -> "UPPER_PRIMARY";
            case "SECONDARY", "SECONDARY_SCHOOL" -> "SECONDARY";
            case "HIGHER_SECONDARY", "SENIOR_SECONDARY", "HIGHER_SECONDARY_SENIOR_SECONDARY" -> "HIGHER_SECONDARY";
            case "POST_DOCTORAL", "POSTDOCTORAL" -> "POSTDOCTORAL";
            case "SCHOOL" -> "SCHOOL";
            case "DIPLOMA" -> "DIPLOMA";
            case "OTHER" -> "OTHER";
            default -> "OTHER";
        };
    }

    private Set<String> schoolLevels() {
        return Set.of("PRIMARY_SCHOOL", "UPPER_PRIMARY", "SECONDARY", "HIGHER_SECONDARY");
    }

    private boolean isOpen(Scholarship scholarship) {
        return scholarship.getDeadline() == null
                || !scholarship.getDeadline().isBefore(LocalDate.now());
    }

    private void apply(Scholarship scholarship, ScholarshipRequest request) {
        scholarship.setName(request.getName());
        scholarship.setProvider(request.getProvider());
        scholarship.setDescription(request.getDescription());
        scholarship.setApplicationUrl(request.getApplicationUrl());
        scholarship.setAmount(request.getAmount());
        scholarship.setDeadline(request.getDeadline());
        scholarship.setEducationLevel(request.getEducationLevel());
        scholarship.setState(request.getState());
        scholarship.setCategory(request.getCategory());
        scholarship.setStatus(request.getStatus());
    }

    private void saveRules(Scholarship scholarship, List<ScholarshipRuleRequest> requestedRules) {
        if (requestedRules == null) {
            return;
        }

        requestedRules.forEach(this::validateRule);
        rules.deleteByScholarship_Id(scholarship.getId());

        for (var requestedRule : requestedRules) {
            var rule = new ScholarshipRule();
            rule.setScholarship(scholarship);
            rule.setRuleType(requestedRule.ruleType().trim().toUpperCase(Locale.ROOT));
            rule.setOperator(requestedRule.operator());
            rule.setRuleValue(requestedRule.ruleValue());
            rule.setDescription(requestedRule.description());
            rules.save(rule);
        }
    }

    private void validateRule(ScholarshipRuleRequest rule) {
        var type = normalizeRuleType(rule.ruleType());
        if (!SUPPORTED_RULE_TYPES.contains(type)) {
            throw new InvalidRuleException("Unsupported scholarship rule type: " + rule.ruleType());
        }
        if (!SUPPORTED_OPERATORS.contains(rule.operator())) {
            throw new InvalidRuleException("Unsupported rule operator: " + rule.operator());
        }

        if (isNumericRule(type)) {
            try {
                new BigDecimal(rule.ruleValue());
            } catch (NumberFormatException exception) {
                throw new InvalidRuleException("Numeric rule value required for " + type);
            }
        }
    }

    private String normalizeRuleType(String ruleType) {
        return ruleType.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private boolean isNumericRule(String ruleType) {
        return List.of(
                "CGPA",
                "ANNUAL_FAMILY_INCOME",
                "INCOME",
                "PREVIOUS_MARKS_PERCENTAGE",
                "PUC_MARKS_PERCENTAGE",
                "DISABILITY_PERCENTAGE",
                "YEAR_OF_STUDY").contains(ruleType);
    }

    private ScholarshipResponse map(Scholarship scholarship) {
        var response = new ScholarshipResponse();
        response.setId(scholarship.getId());
        response.setName(scholarship.getName());
        response.setProvider(scholarship.getProvider());
        response.setDescription(scholarship.getDescription());
        response.setApplicationUrl(scholarship.getApplicationUrl());
        response.setAmount(scholarship.getAmount());
        response.setDeadline(scholarship.getDeadline());
        response.setEducationLevel(scholarship.getEducationLevel());
        response.setState(scholarship.getState());
        response.setCategory(scholarship.getCategory());
        response.setStatus(scholarship.getStatus());
        response.setCreatedAt(scholarship.getCreatedAt());
        response.setUpdatedAt(scholarship.getUpdatedAt());
        return response;
    }

    private ScholarshipRuleResponse mapRule(ScholarshipRule rule) {
        var response = new ScholarshipRuleResponse();
        response.setId(rule.getId());
        response.setRuleType(rule.getRuleType());
        response.setOperator(rule.getOperator());
        response.setRuleValue(rule.getRuleValue());
        response.setDescription(rule.getDescription());
        return response;
    }
}
