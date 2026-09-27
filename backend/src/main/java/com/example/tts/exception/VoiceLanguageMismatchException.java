package com.example.tts.exception;

public class VoiceLanguageMismatchException extends RuntimeException {

    private final String voice;
    private final String language;

    public VoiceLanguageMismatchException(String voice, String language) {
        super(String.format("Voice '%s' is not compatible with language '%s'", voice, language));
        this.voice = voice;
        this.language = language;
    }

    public String getVoice() {
        return voice;
    }

    public String getLanguage() {
        return language;
    }
}
