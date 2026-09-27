package com.example.tts.exception;

/**
 * Exception thrown when an uploaded document type or format is unsupported or invalid.
 */
public class UnsupportedDocumentException extends RuntimeException {
    public UnsupportedDocumentException(String message) {
        super(message);
    }
}
