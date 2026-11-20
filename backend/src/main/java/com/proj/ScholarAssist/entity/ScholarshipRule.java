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

/** One eligibility condition attached to a scholarship. */
@Entity
@Table(name = "scholarship_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScholarshipRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "scholarship_id", nullable = false)
    private Scholarship scholarship;

    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType;

    @Column(nullable = false, length = 10)
    private String operator;

    @Column(name = "rule_value", nullable = false, length = 255)
    private String ruleValue;

    @Column(length = 500)
    private String description;
}
