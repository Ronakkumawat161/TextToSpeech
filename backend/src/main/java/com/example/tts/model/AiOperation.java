package com.example.tts.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

/**
 * Supported AI text enhancement operations.
 */
public enum AiOperation {
    SUMMARIZE,
    CORRECT_GRAMMAR,
    REWRITE,
    CONVERSATIONAL;

    @JsonCreator
    public static AiOperation fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("AI enhancement operation is required.");
        }
        try {
            return AiOperation.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Unsupported AI operation '" + value + "'. Supported operations: SUMMARIZE, CORRECT_GRAMMAR, REWRITE, CONVERSATIONAL."
            );
        }
    }
}
