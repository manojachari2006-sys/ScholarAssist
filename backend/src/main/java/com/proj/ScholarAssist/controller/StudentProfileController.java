package com.proj.ScholarAssist.controller;

import com.proj.ScholarAssist.dto.StudentProfileRequest;
import com.proj.ScholarAssist.dto.StudentProfileResponse;
import com.proj.ScholarAssist.service.StudentProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

/** HTTP endpoints for managing student profiles. */
@RestController
@RequestMapping("/api/student-profile")
public class StudentProfileController {
    private final StudentProfileService service;

    public StudentProfileController(StudentProfileService service) {
        this.service = service;
    }

    /** Creates a profile for an existing user. */
    @PostMapping("/{userId}")
    public ResponseEntity<StudentProfileResponse> create(
            @PathVariable Long userId,
            @Valid
            @RequestBody StudentProfileRequest request,
            Authentication authentication) {
        return ResponseEntity.status(201)
                .body(service.create(userId, request, authentication.getName()));
    }

    /** Retrieves the profile associated with a user. */
    @GetMapping("/{userId}")
    public ResponseEntity<StudentProfileResponse> get(
            @PathVariable Long userId,
            Authentication authentication) {
        return ResponseEntity.ok(service.getByUserId(userId, authentication.getName()));
    }

    /** Replaces the editable profile fields. */
    @PutMapping("/{userId}")
    public ResponseEntity<StudentProfileResponse> update(
            @PathVariable Long userId,
            @Valid
            @RequestBody StudentProfileRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.update(userId, request, authentication.getName()));
    }
}
