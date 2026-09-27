package com.example.tts.exception;

public class InvalidVoiceException extends RuntimeException {

    public InvalidVoiceException(String voice) {
        super("Invalid or unsupported voice: '" + voice + "'");
    }
}
