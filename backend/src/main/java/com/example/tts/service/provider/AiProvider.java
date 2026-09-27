package com.example.tts.service.provider;

import com.example.tts.model.AiOperation;

/**
 * Strategy interface isolating AI text enhancement provider implementations.
 * Decouples controllers and business services from external cloud AI APIs (OpenAI, Gemini, Azure, etc.).
 */
public interface AiProvider {

    /**
     * Enhance input text according to specified operation.
     *
     * @param text      Input text to enhance
     * @param operation The enhancement operation (SUMMARIZE, CORRECT_GRAMMAR, REWRITE, CONVERSATIONAL)
     * @return Enhanced text string
     */
    String enhanceText(String text, AiOperation operation);

    /**
     * Unique name of the active AI provider (e.g. "OPENAI", "GEMINI", "MOCK")
     */
    String getProviderName();

    /**
     * Whether real external provider credentials are valid and active
     */
    boolean isConfigured();
}
