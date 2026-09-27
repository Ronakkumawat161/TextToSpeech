package com.example.tts.controller;

import com.example.tts.dto.TtsRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "testuser@example.com")
class TtsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.example.tts.security.RateLimiterService rateLimiterService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        if (rateLimiterService != null) {
            rateLimiterService.reset();
        }
    }


    @Test
    void testSynthesizeDirectAudioBytes() throws Exception {
        TtsRequest request = TtsRequest.builder()
                .text("Hello, welcome to our application.")
                .language("en-US")
                .voice("en-female-1")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(header().string("Content-Disposition", containsString("speech-en-female-1-")))
                .andExpect(header().string("X-Audio-Format", "audio/mpeg"))
                .andExpect(header().string("X-Voice-Id", "en-female-1"))
                .andExpect(header().string("X-Language", "en-US"));
    }

    @Test
    void testSynthesizeJsonResponseWhenRequested() throws Exception {
        TtsRequest request = TtsRequest.builder()
                .text("Hello, welcome to our application.")
                .language("en-US")
                .voice("en-female-1")
                .build();

        mockMvc.perform(post("/api/tts")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.text").value("Hello, welcome to our application."))
                .andExpect(jsonPath("$.language").value("en-US"))
                .andExpect(jsonPath("$.voice").value("en-female-1"))
                .andExpect(jsonPath("$.format").value("audio/mpeg"))
                .andExpect(jsonPath("$.audioBase64").isNotEmpty());
    }

    @Test
    void testValidationBlankTextFails() throws Exception {
        TtsRequest request = TtsRequest.builder()
                .text("   ")
                .language("en-US")
                .voice("en-female-1")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").exists())
                .andExpect(jsonPath("$.error.details.text").exists());
    }

    @Test
    void testValidationMissingLanguageFails() throws Exception {
        TtsRequest request = TtsRequest.builder()
                .text("Hello world")
                .language(null)
                .voice("en-female-1")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.details.language").exists());
    }

    @Test
    void testValidationMissingVoiceFails() throws Exception {
        TtsRequest request = TtsRequest.builder()
                .text("Hello world")
                .language("en-US")
                .voice("")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.details.voice").exists());
    }

    @Test
    void testValidationExceedsMaxLengthFails() throws Exception {
        String longText = "A".repeat(5001);
        TtsRequest request = TtsRequest.builder()
                .text(longText)
                .language("en-US")
                .voice("en-female-1")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.details.text").value(containsString("cannot exceed 5000 characters")));
    }

    @Test
    void testInvalidLanguageReturnsBadRequest() throws Exception {
        TtsRequest request = TtsRequest.builder()
                .text("Hello world")
                .language("invalid-lang-99")
                .voice("en-female-1")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_LANGUAGE"))
                .andExpect(jsonPath("$.error.message").value(containsString("Invalid or unsupported language")));
    }

    @Test
    void testInvalidVoiceReturnsBadRequest() throws Exception {
        TtsRequest request = TtsRequest.builder()
                .text("Hello world")
                .language("en-US")
                .voice("non-existent-voice-999")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_VOICE"))
                .andExpect(jsonPath("$.error.message").value(containsString("Invalid or unsupported voice")));
    }

    @Test
    void testVoiceLanguageMismatchReturnsBadRequest() throws Exception {
        // Voice is English Female (en-US), but requested language is Hindi (hi-IN)
        TtsRequest request = TtsRequest.builder()
                .text("Namaste world")
                .language("hi-IN")
                .voice("en-female-1")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VOICE_LANGUAGE_MISMATCH"))
                .andExpect(jsonPath("$.error.message").value(containsString("not compatible with language")));
    }

    @Test
    void testUnsupportedMediaTypeFails() throws Exception {
        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.TEXT_PLAIN)
                .content("plain text input"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void testMethodNotAllowedFails() throws Exception {
        mockMvc.perform(get("/api/tts"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void testSynthesizeWithCustomSpeedPitchVolumeStyle() throws Exception {
        TtsRequest request = TtsRequest.builder()
                .text("Custom speech settings test.")
                .language("en-US")
                .voice("en-female-1")
                .speed(1.25)
                .pitch(10)
                .volume(0.85)
                .style("cheerful")
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(header().string("X-Audio-Format", "audio/mpeg"))
                .andExpect(header().string("X-Voice-Id", "en-female-1"));
    }

    @Test
    void testValidationInvalidSpeedReturnsBadRequest() throws Exception {
        TtsRequest request = TtsRequest.builder()
                .text("Invalid speed test")
                .language("en-US")
                .voice("en-female-1")
                .speed(5.5) // Exceeds max 4.0
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void testValidationInvalidPitchReturnsBadRequest() throws Exception {
        TtsRequest request = TtsRequest.builder()
                .text("Invalid pitch test")
                .language("en-US")
                .voice("en-female-1")
                .pitch(150) // Exceeds max 100
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void testValidationInvalidVolumeReturnsBadRequest() throws Exception {
        TtsRequest request = TtsRequest.builder()
                .text("Invalid volume test")
                .language("en-US")
                .voice("en-female-1")
                .volume(-0.5) // Less than min 0.0
                .build();

        mockMvc.perform(post("/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void testGetVoicesIncludesSupportedStyles() throws Exception {
        mockMvc.perform(get("/api/voices"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].supportedStyles").isArray())
                .andExpect(jsonPath("$[0].supportedStyles").isNotEmpty());
    }
}

