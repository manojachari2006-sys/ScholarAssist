package com.proj.ScholarAssist.exception;

/** Indicates that a document action is not allowed in its current state. */
public class InvalidDocumentStateException extends RuntimeException {
    public InvalidDocumentStateException(String message) {
        super(message);
    }
}
