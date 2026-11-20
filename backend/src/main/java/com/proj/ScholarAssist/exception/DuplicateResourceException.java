package com.proj.ScholarAssist.exception;

/** Indicates that a requested operation would create a duplicate record. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
