package com.example.tts.service.provider;

import com.example.tts.config.AiProperties;
import com.example.tts.exception.ApiException;
import com.example.tts.exception.TtsAuthenticationException;
import com.example.tts.exception.TtsTimeoutException;
import com.example.tts.model.AiOperation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Production-ready External AI Text Provider.
 * Communicates with cloud LLM Chat Completions APIs (OpenAI, Gemini, Azure OpenAI, etc.).
 * 
 * Features:
 * - Strict connect & socket read timeouts.
 * - API key kept strictly server-side (never exposed to client).
 * - Comprehensive HTTP status code mapping.
 * - Automatic delegation to MockAiProvider when credentials are not configured.
 */
@Slf4j
@Primary
@Component("externalAiProvider")
public class ExternalAiProviderImpl implements AiProvider {

    private final AiProperties properties;
    private final MockAiProvider mockAiProvider;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ExternalAiProviderImpl(AiProperties properties, MockAiProvider mockAiProvider, ObjectMapper objectMapper) {
        this.properties = properties;
        this.mockAiProvider = mockAiProvider;
        this.objectMapper = objectMapper;

        // Configure client factory with strict timeout controls
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String getProviderName() {
        return isConfigured() ? properties.getProvider().toUpperCase() : mockAiProvider.getProviderName();
    }

    @Override
    public boolean isConfigured() {
        String key = properties.getApiKey();
        return key != null &&
                !key.isBlank() &&
                !key.equalsIgnoreCase("mock") &&
                !key.equalsIgnoreCase("your_ai_api_key_here");
    }

    @Override
    public String enhanceText(String text, AiOperation operation) {
        // If credentials are not configured or provider is explicitly MOCK, delegate to MockAiProvider
        if (!isConfigured() || "MOCK".equalsIgnoreCase(properties.getProvider())) {
            log.info("[ExternalAiProvider] External AI credentials not active (provider={}). Delegating to isolated MockAiProvider.",
                    properties.getProvider());
            return mockAiProvider.enhanceText(text, operation);
        }

        String endpoint = properties.getEndpoint();
        log.info("[ExternalAiProvider] Calling external AI endpoint at '{}' (model='{}', operation='{}')",
                endpoint, properties.getModel(), operation);

        String systemPrompt = getSystemPrompt(operation);
        String userPrompt = "Input text to process:\n" + text;

        Map<String, Object> payload = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.7,
                "max_tokens", 1500
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        headers.set("User-Agent", "VoxFlow-TextToSpeech/1.0");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getBody() == null || response.getBody().isBlank()) {
                log.warn("[ExternalAiProvider] Provider returned empty response body. Falling back to mock output.");
                return mockAiProvider.enhanceText(text, operation);
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText();
                if (content != null && !content.isBlank()) {
                    log.info("[ExternalAiProvider] Successfully enhanced text using external AI (output length: {})", content.trim().length());
                    return content.trim();
                }
            }

            log.warn("[ExternalAiProvider] Unexpected response JSON structure from AI provider: {}. Falling back to mock.", response.getBody());
            return mockAiProvider.enhanceText(text, operation);

        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            log.error("[ExternalAiProvider] Authentication failure ({}): {}", ex.getStatusCode(), ex.getMessage());
            throw new TtsAuthenticationException("Invalid or unauthorized AI API key. Verify AI_API_KEY.");

        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.warn("[ExternalAiProvider] Rate limit exceeded by external AI provider: {}", ex.getMessage());
            throw new ApiException("External AI service rate limit exceeded. Please wait a moment and try again.", HttpStatus.TOO_MANY_REQUESTS);

        } catch (ResourceAccessException ex) {
            if (ex.getCause() instanceof SocketTimeoutException) {
                log.error("[ExternalAiProvider] Request timed out after {}ms: {}", properties.getReadTimeoutMs(), ex.getMessage());
                throw new TtsTimeoutException("AI enhancement request timed out after " + properties.getReadTimeoutMs() + "ms");
            }
            log.error("[ExternalAiProvider] Network connection failed: {}", ex.getMessage());
            throw new ApiException("Unable to establish network connection to external AI provider: " + ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);

        } catch (Exception ex) {
            log.error("[ExternalAiProvider] Unexpected failure during AI enhancement", ex);
            throw new ApiException("AI text enhancement failed: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getSystemPrompt(AiOperation operation) {
        switch (operation) {
            case SUMMARIZE:
                return "You are an expert audio script editor. Condense the user's text into a concise, clear, and informative summary optimized for text-to-speech audio narration. Return only the summary text without commentary.";
            case CORRECT_GRAMMAR:
                return "You are an expert proofreader. Correct all grammatical errors, spelling mistakes, punctuation, and capitalization in the user's text. Preserve the original meaning and tone completely. Return only the corrected text without commentary.";
            case REWRITE:
                return "You are an expert writer. Rewrite the user's text with elegant vocabulary, clear syntax, and engaging sentence structure perfectly suited for spoken audio. Return only the rewritten text without commentary.";
            case CONVERSATIONAL:
                return "You are a warm, engaging podcast host. Rephrase the user's text into natural, friendly, conversational spoken dialogue with natural verbal cadence. Return only the conversational text without commentary.";
            default:
                return "You are a helpful text editor. Improve the provided text for speech synthesis.";
        }
    }
}
