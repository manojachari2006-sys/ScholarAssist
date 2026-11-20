package com.proj.ScholarAssist.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Public view of an eligibility rule, without its scholarship entity reference. */
@Getter
@Setter
@NoArgsConstructor
public class ScholarshipRuleResponse {
    private Long id;
    private String ruleType;
    private String operator;
    private String ruleValue;
    private String description;
}
