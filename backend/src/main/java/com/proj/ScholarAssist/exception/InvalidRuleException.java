package com.proj.ScholarAssist.exception;

/** Indicates that a scholarship eligibility rule is unsupported or malformed. */
public class InvalidRuleException extends RuntimeException {
    public InvalidRuleException(String message) {
        super(message);
    }
}
