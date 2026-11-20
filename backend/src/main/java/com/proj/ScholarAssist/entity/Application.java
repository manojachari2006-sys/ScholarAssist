package com.proj.ScholarAssist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** JPA record connecting a student profile to a scholarship application. */
@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning student profile; a profile may submit multiple applications. */
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    /** Scholarship being applied for. */
    @ManyToOne
    @JoinColumn(name = "scholarship_id", nullable = false)
    private Scholarship scholarship;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    @Column(nullable = false, length = 50)
    private String status = "DRAFT";

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "provider_application_id", length = 150)
    private String providerApplicationId;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
