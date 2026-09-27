package com.example.tts.exception;

public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException() {
        super("Text-to-speech service is temporarily unavailable. Please try again later.");
    }
}
