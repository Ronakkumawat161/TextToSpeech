package com.example.tts.exception;

public class TtsTimeoutException extends RuntimeException {

    public TtsTimeoutException(String message) {
        super(message);
    }
}
