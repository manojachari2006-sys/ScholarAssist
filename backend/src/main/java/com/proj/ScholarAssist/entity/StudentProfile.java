package com.proj.ScholarAssist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Student details linked one-to-one to the owning user. */
@Entity
@Getter
@Setter
@Table(name = "student_profiles")
@AllArgsConstructor
@NoArgsConstructor
public class StudentProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, name = "full_name", length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 30)
    private String gender;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String district;

    @Column(name = "college_name", length = 200)
    private String collegeName;

    @Column(length = 150)
    private String course;

    @Column(name = "year_of_study")
    private Integer yearOfStudy;

    @Column(precision = 4, scale = 2)
    private BigDecimal cgpa;

    @Column(name = "annual_family_income", precision = 12, scale = 2)
    private BigDecimal annualFamilyIncome;

    @Column(length = 50)
    private String category;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;
}
