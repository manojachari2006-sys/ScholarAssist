package com.proj.ScholarAssist.service;

import com.proj.ScholarAssist.dto.StudentProfileRequest;
import com.proj.ScholarAssist.dto.StudentProfileResponse;
import com.proj.ScholarAssist.entity.StudentProfile;
import com.proj.ScholarAssist.exception.DuplicateResourceException;
import com.proj.ScholarAssist.exception.ResourceNotFoundException;
import com.proj.ScholarAssist.repository.StudentProfileRepository;
import com.proj.ScholarAssist.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;

/** Creates and updates student profiles linked to existing users. */
@Service
public class StudentProfileService {
    private final StudentProfileRepository profiles;
    private final UserRepository users;

    public StudentProfileService(
            StudentProfileRepository profiles,
            UserRepository users) {
        this.profiles = profiles;
        this.users = users;
    }

    /**
     * Creates one profile for an existing user. The user lookup also ensures the
     * profile's foreign key points to a real account.
     */
    @Transactional
    public StudentProfileResponse create(
            Long userId,
            StudentProfileRequest request,
            String authenticatedEmail) {
        var user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        requireOwner(user.getEmail(), authenticatedEmail);

        if (profiles.findByUser_Id(userId).isPresent()) {
            throw new DuplicateResourceException("A profile already exists for this user");
        }

        var profile = new StudentProfile();
        profile.setUser(user);
        apply(profile, request);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        return map(profiles.save(profile));
    }

    /** Retrieves the profile by its owning user's ID. */
    @Transactional(readOnly = true)
    public StudentProfileResponse getByUserId(Long userId, String authenticatedEmail) {
        var user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        requireOwner(user.getEmail(), authenticatedEmail);
        return profiles.findByUser_Id(userId)
                .map(this::map)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student profile not found for user: " + userId));
    }

    /** Updates all editable profile fields while preserving its user relationship. */
    @Transactional
    public StudentProfileResponse update(
            Long userId,
            StudentProfileRequest request,
            String authenticatedEmail) {
        var profile = profiles.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student profile not found for user: " + userId));
        requireOwner(profile.getUser().getEmail(), authenticatedEmail);
        apply(profile, request);
        profile.setUpdatedAt(LocalDateTime.now());
        return map(profile);
    }

    private void requireOwner(String ownerEmail, String authenticatedEmail) {
        if (!ownerEmail.equalsIgnoreCase(authenticatedEmail)) {
            throw new AccessDeniedException(
                    "This student profile does not belong to the signed-in account");
        }
    }

    private void apply(StudentProfile profile, StudentProfileRequest request) {
        profile.setFullName(request.getFullName());
        profile.setPhone(request.getPhone());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setState(request.getState());
        profile.setDistrict(request.getDistrict());
        profile.setCollegeName(request.getCollegeName());
        profile.setCourse(request.getCourse());
        profile.setYearOfStudy(request.getYearOfStudy());
        profile.setCgpa(request.getCgpa());
        profile.setAnnualFamilyIncome(request.getAnnualFamilyIncome());
        profile.setCategory(request.getCategory());
    }

    private StudentProfileResponse map(StudentProfile profile) {
        var response = new StudentProfileResponse();
        response.setId(profile.getId());
        response.setUserId(profile.getUser().getId());
        response.setFullName(profile.getFullName());
        response.setPhone(profile.getPhone());
        response.setDateOfBirth(profile.getDateOfBirth());
        response.setGender(profile.getGender());
        response.setState(profile.getState());
        response.setDistrict(profile.getDistrict());
        response.setCollegeName(profile.getCollegeName());
        response.setCourse(profile.getCourse());
        response.setYearOfStudy(profile.getYearOfStudy());
        response.setCgpa(profile.getCgpa());
        response.setAnnualFamilyIncome(profile.getAnnualFamilyIncome());
        response.setCategory(profile.getCategory());
        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());
        return response;
    }
}
