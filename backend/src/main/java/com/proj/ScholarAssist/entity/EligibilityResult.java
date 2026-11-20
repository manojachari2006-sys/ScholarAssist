package com.proj.ScholarAssist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/** Latest saved eligibility evaluation for one student and scholarship pair. */
@Entity
@Table(
        name = "eligibility_results",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"student_id", "scholarship_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @ManyToOne
    @JoinColumn(name = "scholarship_id", nullable = false)
    private Scholarship scholarship;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;
}
