package com.example.tts.util;

/**
 * Security utility for sanitizing log outputs.
 * Enforces privacy protection:
 * - Prevents logging of API keys and provider credentials.
 * - Prevents logging of full user text payloads that may contain sensitive user data.
 * - Masks Authorization headers.
 */
public final class LogSanitizer {

    private LogSanitizer() {
        // Utility class
    }

    /**
     * Sanitize user text by only returning metadata/length to prevent logging PII or sensitive text.
     */
    public static String summarizeText(String text) {
        if (text == null) {
            return "[null]";
        }
        return "[textLength=" + text.length() + " chars]";
    }

    /**
     * Mask API keys or sensitive credentials.
     * E.g. "sk-1234567890abcdef" -> "sk-1***cdef"
     */
    public static String maskSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return "[not configured]";
        }
        if (secret.length() <= 8) {
            return "******";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }
}
