package com.example.tts.service;

import com.example.tts.dto.AiEnhanceRequest;
import com.example.tts.dto.AiEnhanceResponse;

/**
 * Service abstraction for AI-assisted text enhancements (SUMMARIZE, CORRECT_GRAMMAR, REWRITE, CONVERSATIONAL).
 */
public interface AiTextService {

    /**
     * Enhances input text according to the requested AI operation.
     *
     * @param request Validated AI enhancement request
     * @return AI enhancement response containing original and enhanced text
     */
    AiEnhanceResponse enhanceText(AiEnhanceRequest request);
}
