package com.example.tts.controller;

import com.example.tts.dto.AudioSynthesisResult;
import com.example.tts.dto.TtsRequest;
import com.example.tts.dto.VoiceResponse;
import com.example.tts.exception.*;
import com.example.tts.service.TtsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive Backend Testing Suite verifying all error handling,
 * authentication, provider failures, rate limiting, and edge case validations.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TtsComprehensiveTestingSuiteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TtsService ttsService;

    @MockBean
    private com.example.tts.service.SpeechHistoryService speechHistoryService;

    // -------------------------------------------------------------------------
    // 1. HEALTH ENDPOINT
    // -------------------------------------------------------------------------
    @Test
    void testHealthEndpointPublicAccess() throws Exception {
        mockMvc.perform(get("/api/health")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // 2. VOICES ENDPOINT
    // -------------------------------------------------------------------------
    @Test
    void testVoicesEndpointReturnsCatalog() throws Exception {
        VoiceResponse voice1 = VoiceResponse.builder()
                .id("en-female-1")
                .name("Jenny")
                .language("en-US")
                .gender("Female")
                .supportedStyles(List.of("default", "cheerful"))
                .build();

        when(ttsService.getAvailableVoices()).thenReturn(List.of(voice1));

        mockMvc.perform(get("/api/voices")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("en-female-1"))
                .andExpect(jsonPath("$[0].name").value("Jenny"))
                .andExpect(jsonPath("$[0].language").value("en-US"));
    }

    // -------------------------------------------------------------------------
    // 3. AUTHENTICATION PROTECTION ON TTS ENDPOINT
    // -------------------------------------------------------------------------
    @Test
    void testTtsWithoutAuthenticationReturns401Unauthorized() throws Exception {
        TtsRequest request = TtsRequest.builder()
                .text("Valid speech text")
                .language("en-US")
                .voice("en-female-1")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    // -------------------------------------------------------------------------
    // 4. TTS SUCCESS WITH AUTHENTICATION
    // -------------------------------------------------------------------------
    @Test
    @WithMockUser(username = "alice@example.com", roles = {"USER"})
    void testTtsWithAuthenticationReturnsAudioStream() throws Exception {
        TtsRequest request = TtsRequest.builder()
                .text("Valid speech text for synthesis")
                .language("en-US")
                .voice("en-female-1")
                .build();

        AudioSynthesisResult result = AudioSynthesisResult.builder()
                .audioBytes(new byte[]{0x49, 0x44, 0x33, 0x03})
                .format("audio/mpeg")
                .filename("speech-en-female-1-test.mp3")
                .voice("en-female-1")
                .language("en-US")
                .characterCount(31)
                .provider("MOCK")
                .build();

        when(ttsService.synthesizeAudio(any(TtsRequest.class))).thenReturn(result);

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(header().string("X-Voice-Id", "en-female-1"))
                .andExpect(header().string("X-Language", "en-US"));
    }

    // -------------------------------------------------------------------------
    // 5. VALIDATION: EMPTY AND EXCESSIVE TEXT
    // -------------------------------------------------------------------------
    @Test
    @WithMockUser(username = "alice@example.com")
    void testTtsValidationEmptyStringReturns400() throws Exception {
        TtsRequest request = TtsRequest.builder()
                .text("")
                .language("en-US")
                .voice("en-female-1")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void testTtsValidationWhitespaceOnlyReturns400() throws Exception {
        TtsRequest request = TtsRequest.builder()
                .text("     ")
                .language("en-US")
                .voice("en-female-1")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void testTtsValidationExcessiveTextReturns400() throws Exception {
        String excessiveText = "X".repeat(5001);
        TtsRequest request = TtsRequest.builder()
                .text(excessiveText)
                .language("en-US")
                .voice("en-female-1")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.details.text", containsString("cannot exceed 5000 characters")));
    }

    // -------------------------------------------------------------------------
    // 6. VALIDATION: INVALID LANGUAGE AND INVALID VOICE
    // -------------------------------------------------------------------------
    @Test
    @WithMockUser(username = "alice@example.com")
    void testTtsInvalidLanguageThrowsInvalidLanguageExceptionReturns400() throws Exception {
        when(ttsService.synthesizeAudio(any(TtsRequest.class)))
                .thenThrow(new InvalidLanguageException("xx-YY"));

        TtsRequest request = TtsRequest.builder()
                .text("Test text")
                .language("xx-YY")
                .voice("en-female-1")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_LANGUAGE"))
                .andExpect(jsonPath("$.error.message", containsString("xx-YY")));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void testTtsInvalidVoiceThrowsInvalidVoiceExceptionReturns400() throws Exception {
        when(ttsService.synthesizeAudio(any(TtsRequest.class)))
                .thenThrow(new InvalidVoiceException("bogus-voice-999"));

        TtsRequest request = TtsRequest.builder()
                .text("Test text")
                .language("en-US")
                .voice("bogus-voice-999")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_VOICE"))
                .andExpect(jsonPath("$.error.message", containsString("bogus-voice-999")));
    }

    // -------------------------------------------------------------------------
    // 7. PROVIDER FAILURE (HTTP 502 BAD GATEWAY)
    // -------------------------------------------------------------------------
    @Test
    @WithMockUser(username = "alice@example.com")
    void testTtsProviderFailureThrowsTtsSynthesisExceptionReturns502() throws Exception {
        when(ttsService.synthesizeAudio(any(TtsRequest.class)))
                .thenThrow(new TtsSynthesisException("Speech provider synthesis failed"));

        TtsRequest request = TtsRequest.builder()
                .text("Valid synthesis text")
                .language("en-US")
                .voice("en-female-1")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SYNTHESIS_FAILED"))
                .andExpect(jsonPath("$.error.message", containsString("Failed to synthesize audio with speech provider")));
    }

    // -------------------------------------------------------------------------
    // 8. SERVICE UNAVAILABLE (HTTP 503 SERVICE UNAVAILABLE)
    // -------------------------------------------------------------------------
    @Test
    @WithMockUser(username = "alice@example.com")
    void testTtsProviderUnavailableThrowsTtsProviderUnavailableExceptionReturns503() throws Exception {
        when(ttsService.synthesizeAudio(any(TtsRequest.class)))
                .thenThrow(new TtsProviderUnavailableException("Azure speech service unavailable"));

        TtsRequest request = TtsRequest.builder()
                .text("Valid synthesis text")
                .language("en-US")
                .voice("en-female-1")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.error.message", containsString("temporarily unavailable")));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    void testTtsTimeoutThrowsTtsTimeoutExceptionReturns503() throws Exception {
        when(ttsService.synthesizeAudio(any(TtsRequest.class)))
                .thenThrow(new TtsTimeoutException("Connection timed out after 5000ms"));

        TtsRequest request = TtsRequest.builder()
                .text("Valid synthesis text")
                .language("en-US")
                .voice("en-female-1")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.error.message", containsString("timed out")));
    }

    // -------------------------------------------------------------------------
    // 9. RATE LIMITING (HTTP 429 TOO MANY REQUESTS)
    // -------------------------------------------------------------------------
    @Test
    @WithMockUser(username = "alice@example.com")
    void testTtsRateLimitingThrowsRateLimitExceededReturns429() throws Exception {
        when(ttsService.synthesizeAudio(any(TtsRequest.class)))
                .thenThrow(new RateLimitExceededException("Rate limit of 10 requests per minute exceeded. Please wait."));

        TtsRequest request = TtsRequest.builder()
                .text("Valid synthesis text")
                .language("en-US")
                .voice("en-female-1")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.error.message", containsString("Rate limit of 10 requests per minute exceeded")));
    }

    // -------------------------------------------------------------------------
    // 10. AUTHENTICATION & HISTORY AUTHORIZATION
    // -------------------------------------------------------------------------
    @Test
    void testHistoryWithoutAuthenticationReturns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/history")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(username = "eve@example.com")
    void testHistoryGetUnauthorizedAccessThrowsForbiddenExceptionReturns403() throws Exception {
        when(speechHistoryService.getHistoryById(99L))
                .thenThrow(new ForbiddenException("You are not authorized to view this speech history record."));

        mockMvc.perform(get("/api/history/99")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message", containsString("not authorized to view this speech history record")));
    }

    @Test
    @WithMockUser(username = "eve@example.com")
    void testHistoryDeleteUnauthorizedAccessThrowsForbiddenExceptionReturns403() throws Exception {
        doThrow(new ForbiddenException("You are not authorized to delete another user's speech record."))
                .when(speechHistoryService).deleteHistoryById(99L);

        mockMvc.perform(delete("/api/history/99")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message", containsString("not authorized to delete another user's speech record")));
    }
}
