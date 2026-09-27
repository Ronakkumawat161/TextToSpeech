package com.example.tts.service.provider;

import com.example.tts.model.AiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mock AI Provider implementation.
 * Provides realistic offline text enhancement for development, testing, and demo environments
 * without requiring external cloud AI credentials or incurring API costs.
 */
@Slf4j
@Component("mockAiProvider")
public class MockAiProvider implements AiProvider {

    private static final String PROVIDER_NAME = "MOCK";

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String enhanceText(String text, AiOperation operation) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String input = text.trim();
        log.info("[MockAiProvider] Enhancing text using operation '{}' (input length: {})", operation, input.length());

        switch (operation) {
            case SUMMARIZE:
                return generateSummary(input);
            case CORRECT_GRAMMAR:
                return correctGrammar(input);
            case REWRITE:
                return rewriteText(input);
            case CONVERSATIONAL:
                return makeConversational(input);
            default:
                return input;
        }
    }

    /**
     * Extracts core points and creates a concise spoken summary.
     */
    private String generateSummary(String text) {
        String[] sentences = text.split("(?<=[.!?])\\s+");
        if (sentences.length <= 2) {
            // Already concise; condense into one clean summary sentence
            String cleaned = text.replaceAll("\\s+", " ").trim();
            if (!cleaned.endsWith(".")) cleaned += ".";
            return "In summary: " + cleaned;
        }

        // Take the most significant sentences (e.g. first sentence and last/middle key sentence)
        StringBuilder summary = new StringBuilder();
        summary.append(sentences[0].trim());

        if (sentences.length > 2) {
            summary.append(" Key takeaway: ").append(sentences[sentences.length - 1].trim());
        }

        return summary.toString();
    }

    /**
     * Fixes common grammatical issues, punctuation, capitalization, and spacing.
     */
    private String correctGrammar(String text) {
        String result = text.trim();

        // 1. Fix common spelling errors & contractions
        Map<String, String> replacements = Map.ofEntries(
                Map.entry("(?i)\\bteh\\b", "the"),
                Map.entry("(?i)\\bdont\\b", "don't"),
                Map.entry("(?i)\\bcant\\b", "can't"),
                Map.entry("(?i)\\bwont\\b", "won't"),
                Map.entry("(?i)\\bshouldnt\\b", "shouldn't"),
                Map.entry("(?i)\\bcouldnt\\b", "couldn't"),
                Map.entry("(?i)\\bwouldnt\\b", "wouldn't"),
                Map.entry("(?i)\\bive\\b", "I've"),
                Map.entry("(?i)\\bim\\b", "I'm"),
                Map.entry("(?i)\\btheyre\\b", "they're"),
                Map.entry("(?i)\\byoure\\b", "you're"),
                Map.entry("(?i)\\bits a\\b", "it's a"),
                Map.entry("(?i)\\bi\\b", "I"),
                Map.entry("(?i)\\brecieve\\b", "receive"),
                Map.entry("(?i)\\bseperate\\b", "separate")
        );

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replaceAll(entry.getKey(), entry.getValue());
        }

        // 2. Fix multiple consecutive punctuation (e.g. "???" -> "?", ",," -> ",")
        result = result.replaceAll("!{2,}", "!");
        result = result.replaceAll("\\?{2,}", "?");
        result = result.replaceAll(",{2,}", ",");

        // 3. Ensure single space after punctuation
        result = result.replaceAll("([,.!?;:])(?=[^\\s0-9])", "$1 ");

        // 4. Collapse multiple spaces
        result = result.replaceAll("[ \\t]+", " ");

        // 5. Capitalize first letter of each sentence
        Pattern sentenceStartPattern = Pattern.compile("(^|[.!?]\\s+)([a-z])");
        Matcher matcher = sentenceStartPattern.matcher(result);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1) + matcher.group(2).toUpperCase(Locale.ROOT));
        }
        matcher.appendTail(sb);
        result = sb.toString();

        // 6. Ensure terminal punctuation
        if (!result.isEmpty() && !result.matches(".*[.!?]$")) {
            result += ".";
        }

        return result;
    }

    /**
     * Rewrites text with enhanced vocabulary, clearer phrasing, and elegant flow.
     */
    private String rewriteText(String text) {
        String grammarFixed = correctGrammar(text);

        // Polish sentence starters and smooth transitions
        if (!grammarFixed.startsWith("Notably, ") && !grammarFixed.startsWith("Clearly, ")) {
            return "Essentially, " + Character.toLowerCase(grammarFixed.charAt(0)) + grammarFixed.substring(1);
        }

        return grammarFixed;
    }

    /**
     * Rephrases text into an engaging, natural spoken dialogue with friendly conversational rhythm.
     */
    private String makeConversational(String text) {
        String base = correctGrammar(text);

        // Remove overly stiff corporate phrasing if any
        base = base.replace("In accordance with", "As we talked about,")
                   .replace("Furthermore,", "Also,")
                   .replace("Consequently,", "So,")
                   .replace("It is recommended that", "I'd suggest that");

        String firstCharLower = Character.toLowerCase(base.charAt(0)) + base.substring(1);

        // Prepend friendly conversational opening appropriate for audio dialogue
        List<String> conversationalOpeners = List.of(
                "You know, " + firstCharLower,
                "Here's the thing: " + firstCharLower,
                "To put it simply, " + firstCharLower
        );

        return conversationalOpeners.get(Math.abs(base.hashCode()) % conversationalOpeners.size());
    }
}
