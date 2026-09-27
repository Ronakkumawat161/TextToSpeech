package com.example.tts.controller;

import com.example.tts.dto.AiEnhanceRequest;
import com.example.tts.model.AiOperation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.example.tts.security.RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        if (rateLimiterService != null) {
            rateLimiterService.reset();
        }
    }

    @Test
    void testEnhanceSummarizeSuccess() throws Exception {
        AiEnhanceRequest request = AiEnhanceRequest.builder()
                .text("Artificial intelligence has revolutionized speech synthesis. Machine learning allows natural prosody and voice cloning. Modern TTS systems are fast and clear.")
                .operation(AiOperation.SUMMARIZE)
                .build();

        mockMvc.perform(post("/api/ai/enhance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.originalText").value(request.getText()))
                .andExpect(jsonPath("$.enhancedText").isNotEmpty())
                .andExpect(jsonPath("$.operation").value("SUMMARIZE"));
    }

    @Test
    void testEnhanceCorrectGrammarSuccess() throws Exception {
        AiEnhanceRequest request = AiEnhanceRequest.builder()
                .text("teh speech synthesis app dont crash when i test it")
                .operation(AiOperation.CORRECT_GRAMMAR)
                .build();

        mockMvc.perform(post("/api/ai/enhance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.enhancedText").value(containsString("The speech synthesis app don't crash when I test it.")))
                .andExpect(jsonPath("$.operation").value("CORRECT_GRAMMAR"));
    }

    @Test
    void testEnhanceRewriteSuccess() throws Exception {
        AiEnhanceRequest request = AiEnhanceRequest.builder()
                .text("This application turns text into speech.")
                .operation(AiOperation.REWRITE)
                .build();

        mockMvc.perform(post("/api/ai/enhance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.enhancedText").isNotEmpty())
                .andExpect(jsonPath("$.operation").value("REWRITE"));
    }

    @Test
    void testEnhanceConversationalSuccess() throws Exception {
        AiEnhanceRequest request = AiEnhanceRequest.builder()
                .text("The weather in San Francisco is foggy today.")
                .operation(AiOperation.CONVERSATIONAL)
                .build();

        mockMvc.perform(post("/api/ai/enhance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.enhancedText").isNotEmpty())
                .andExpect(jsonPath("$.operation").value("CONVERSATIONAL"));
    }

    @Test
    void testValidationBlankTextFails() throws Exception {
        AiEnhanceRequest request = AiEnhanceRequest.builder()
                .text("   ")
                .operation(AiOperation.SUMMARIZE)
                .build();

        mockMvc.perform(post("/api/ai/enhance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void testValidationExceedsMaxLengthFails() throws Exception {
        String longText = "A".repeat(5001);
        AiEnhanceRequest request = AiEnhanceRequest.builder()
                .text(longText)
                .operation(AiOperation.SUMMARIZE)
                .build();

        mockMvc.perform(post("/api/ai/enhance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.details.text").value(containsString("cannot exceed 5000 characters")));
    }

    @Test
    void testValidationMissingOperationFails() throws Exception {
        String json = "{\"text\": \"Some text to enhance\"}";

        mockMvc.perform(post("/api/ai/enhance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void testValidationInvalidOperationFails() throws Exception {
        String json = "{\"text\": \"Some text to enhance\", \"operation\": \"INVALID_OPERATION\"}";

        mockMvc.perform(post("/api/ai/enhance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }
}
