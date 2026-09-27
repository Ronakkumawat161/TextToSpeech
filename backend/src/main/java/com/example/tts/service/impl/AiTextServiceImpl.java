package com.example.tts.service.impl;

import com.example.tts.dto.AiEnhanceRequest;
import com.example.tts.dto.AiEnhanceResponse;
import com.example.tts.exception.InvalidRequestException;
import com.example.tts.service.AiTextService;
import com.example.tts.service.provider.AiProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTextServiceImpl implements AiTextService {

    private final AiProvider aiProvider;

    public static final int MAX_TEXT_LENGTH = 5000;

    @Override
    public AiEnhanceResponse enhanceText(AiEnhanceRequest request) {
        // 1. Request null validation
        if (request == null) {
            throw new InvalidRequestException("Request body cannot be null.");
        }

        // 2. Text validation: presence, blank, and maximum character length
        if (request.getText() == null || request.getText().isEmpty()) {
            throw new InvalidRequestException("Text is required and cannot be empty.");
        }
        if (request.getText().trim().isEmpty()) {
            throw new InvalidRequestException("Text cannot consist of whitespace only.");
        }
        if (request.getText().length() > MAX_TEXT_LENGTH) {
            throw new InvalidRequestException("Text length cannot exceed " + MAX_TEXT_LENGTH + " characters.");
        }

        // 3. Operation validation
        if (request.getOperation() == null) {
            throw new InvalidRequestException("Operation is required (supported: SUMMARIZE, CORRECT_GRAMMAR, REWRITE, CONVERSATIONAL).");
        }

        String originalText = request.getText().trim();
        log.info("Processing AI text enhancement: operation='{}', textLength={}, provider='{}'",
                request.getOperation(), originalText.length(), aiProvider.getProviderName());

        // 4. Delegate enhancement to active AI provider abstraction
        String enhancedText = aiProvider.enhanceText(originalText, request.getOperation());

        if (enhancedText == null || enhancedText.isBlank()) {
            log.warn("AI enhancement returned empty text. Falling back to original text.");
            enhancedText = originalText;
        }

        return AiEnhanceResponse.builder()
                .success(true)
                .originalText(originalText)
                .enhancedText(enhancedText.trim())
                .operation(request.getOperation())
                .provider(aiProvider.getProviderName())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
