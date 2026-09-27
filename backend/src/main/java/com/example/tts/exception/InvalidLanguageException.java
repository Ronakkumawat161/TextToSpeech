package com.example.tts.exception;

public class InvalidLanguageException extends RuntimeException {

    public InvalidLanguageException(String language) {
        super("Invalid or unsupported language: '" + language + "'");
    }
}
