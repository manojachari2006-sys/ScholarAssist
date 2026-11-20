package com.proj.ScholarAssist.controller;

import com.proj.ScholarAssist.dto.ScholarshipRequest;
import com.proj.ScholarAssist.dto.ScholarshipResponse;
import com.proj.ScholarAssist.dto.ScholarshipRuleRequest;
import com.proj.ScholarAssist.dto.ScholarshipRuleResponse;
import com.proj.ScholarAssist.service.ScholarshipService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/** HTTP endpoints for searching and maintaining scholarships. */
@RestController
@RequestMapping("/api/scholarships")
public class ScholarshipController {
    private final ScholarshipService service;

    public ScholarshipController(ScholarshipService service) {
        this.service = service;
    }

    /** Lists scholarships matching any supplied filters. */
    @GetMapping
    public ResponseEntity<List<ScholarshipResponse>> all(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String educationLevel) {
        return ResponseEntity.ok(service.all(state, category, educationLevel));
    }

    /** Searches scholarship names and providers, then applies optional filters. */
    @GetMapping("/search")
    public ResponseEntity<List<ScholarshipResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String educationLevel) {
        return ResponseEntity.ok(service.search(q, state, category, educationLevel));
    }

    /** Retrieves one scholarship. */
    @GetMapping("/{id}")
    public ResponseEntity<ScholarshipResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    /** Lists the eligibility rules for a scholarship. */
    @GetMapping("/{id}/rules")
    public ResponseEntity<List<ScholarshipRuleResponse>> rules(@PathVariable Long id) {
        return ResponseEntity.ok(service.getRules(id));
    }

    /** Replaces the eligibility rules for a scholarship. */
    @PutMapping("/{id}/rules")
    public ResponseEntity<List<ScholarshipRuleResponse>> replaceRules(
            @PathVariable Long id,
            @Valid
            @RequestBody List<@Valid ScholarshipRuleRequest> requestedRules) {
        return ResponseEntity.ok(service.replaceRules(id, requestedRules));
    }

    /** Creates a scholarship. */
    @PostMapping
    public ResponseEntity<ScholarshipResponse> create(
            @Valid
            @RequestBody ScholarshipRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    /** Replaces a scholarship and, when supplied, its eligibility rules. */
    @PutMapping("/{id}")
    public ResponseEntity<ScholarshipResponse> update(
            @PathVariable Long id,
            @Valid
            @RequestBody ScholarshipRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    /** Deletes a scholarship. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
