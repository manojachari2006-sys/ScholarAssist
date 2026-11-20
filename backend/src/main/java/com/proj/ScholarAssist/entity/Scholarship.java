package com.proj.ScholarAssist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Scholarship details and lifecycle timestamps. */
@Entity
@Table(name = "scholarships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Scholarship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 200)
    private String provider;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Official provider application page shown to students as the apply destination. */
    @Column(name = "application_url", length = 1000)
    private String applicationUrl;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    private LocalDate deadline;

    @Column(name = "education_level", length = 100)
    private String educationLevel;

    @Column(length = 100)
    private String state;

    @Column(length = 50)
    private String category;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
