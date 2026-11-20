package com.proj.ScholarAssist.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Profile response containing profile and owning user IDs, not JPA entities. */
@Getter
@Setter
@NoArgsConstructor
public class StudentProfileResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String state;
    private String district;
    private String collegeName;
    private String course;
    private Integer yearOfStudy;
    private BigDecimal cgpa;
    private BigDecimal annualFamilyIncome;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
