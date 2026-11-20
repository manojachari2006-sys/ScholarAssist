package com.proj.ScholarAssist.exception;

/** Indicates that a requested database resource does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
