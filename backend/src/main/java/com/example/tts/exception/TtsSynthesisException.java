package com.example.tts.exception;

public class TtsSynthesisException extends RuntimeException {

    public TtsSynthesisException(String message) {
        super(message);
    }

    public TtsSynthesisException(String message, Throwable cause) {
        super(message, cause);
    }
}
