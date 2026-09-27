package com.example.tts.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when login credentials do not match any user or are incorrect.
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super("Invalid email or password", HttpStatus.UNAUTHORIZED);
    }

    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
