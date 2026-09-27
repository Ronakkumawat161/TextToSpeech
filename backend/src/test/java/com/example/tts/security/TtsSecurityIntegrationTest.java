package com.example.tts.security;

import com.example.tts.dto.TtsRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "securitytest@example.com")
class TtsSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService.reset();
    }

    @Test
    void testSecurityHeadersPresentOnResponses() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    void testAllowedCorsOriginHeaderReturned() throws Exception {
        mockMvc.perform(options("/api/tts")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void testUnauthorizedCorsOriginRejected() throws Exception {
        mockMvc.perform(options("/api/tts")
                .header("Origin", "http://malicious-site.com")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void testRateLimitEnforcementReturns429WhenExhausted() throws Exception {
        TtsRequest request = TtsRequest.builder()
                .text("Rate limit test synthesis text")
                .language("en-US")
                .voice("en-female-1")
                .build();

        String jsonPayload = objectMapper.writeValueAsString(request);
        String clientIp = "192.0.2.45";

        // Burst capacity is 10 in application.yml. Fire 11 requests.
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/tts")
                    .with(req -> {
                        req.setRemoteAddr(clientIp);
                        return req;
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonPayload))
                    .andExpect(status().isOk());
        }

        // 11th request must receive HTTP 429 Too Many Requests
        mockMvc.perform(post("/api/tts")
                .with(req -> {
                    req.setRemoteAddr(clientIp);
                    return req;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.error.message").value(containsString("Rate limit exceeded")));
    }
}
