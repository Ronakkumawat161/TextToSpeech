package com.example.tts.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to register with an email that is already registered.
 */
public class UserAlreadyExistsException extends ApiException {

    public UserAlreadyExistsException(String email) {
        super("An account with email '" + email + "' already exists", HttpStatus.CONFLICT);
    }
}
