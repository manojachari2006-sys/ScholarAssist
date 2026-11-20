package com.proj.ScholarAssist.exception;

/** Indicates that an uploaded file could not be validated or stored. */
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
