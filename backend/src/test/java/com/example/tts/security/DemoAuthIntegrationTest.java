package com.example.tts.security;

import com.example.tts.config.DemoAuthProperties;
import com.example.tts.dto.LoginRequest;
import com.example.tts.dto.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test suite for Development Demo Account Authentication.
 * Verifies demo login fallback, BCrypt password matching, JWT generation,
 * endpoint protection, validation error handling, and production isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DemoAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DemoAuthProperties demoAuthProperties;

    @Test
    @DisplayName("TEST 1: Demo login succeeds with valid demo credentials and issues valid JWT")
    void testDemoLoginSuccess() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("demo@textspeech.local")
                .password("Demo@12345")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("demo@textspeech.local"))
                .andExpect(jsonPath("$.user.name").value("Demo User"))
                .andExpect(jsonPath("$.message").value("Authentication successful"));
    }

    @Test
    @DisplayName("TEST 2: Demo login with incorrect password returns 401 Unauthorized")
    void testDemoLoginWrongPasswordFails() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("demo@textspeech.local")
                .password("WrongPassword999!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("TEST 3: Non-demo user with demo password fails authentication")
    void testNonDemoEmailFails() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("unknown.user@example.com")
                .password("Demo@12345")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("TEST 4: Empty email returns 400 Bad Request with validation errors")
    void testEmptyEmailValidationFails() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("")
                .password("Demo@12345")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("TEST 5: Empty password returns 400 Bad Request with validation errors")
    void testEmptyPasswordValidationFails() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("demo@textspeech.local")
                .password("")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("TEST 6: JWT issued from demo login authorizes access to protected /api/history")
    void testDemoJwtAccessToProtectedHistory() throws Exception {
        // 1. Authenticate demo user to get JWT
        LoginRequest loginRequest = LoginRequest.builder()
                .email("demo@textspeech.local")
                .password("Demo@12345")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = responseNode.get("token").asText();
        assertNotNull(token);

        // 2. Use JWT to access protected GET /api/history
        mockMvc.perform(get("/api/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("TEST 7: JWT issued from demo login authorizes access to GET /api/auth/me")
    void testDemoJwtAccessToCurrentUserProfile() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("demo@textspeech.local")
                .password("Demo@12345")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = responseNode.get("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("demo@textspeech.local"))
                .andExpect(jsonPath("$.name").value("Demo User"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("TEST 8: Registration with reserved demo email is rejected")
    void testRegisterReservedDemoEmailFails() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Fake Demo")
                .email("demo@textspeech.local")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("USER_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("TEST 9: DemoAuthProperties is configured and active in development test profile")
    void testDemoAuthPropertiesConfigured() {
        assertTrue(demoAuthProperties.isEnabled(), "Demo authentication should be enabled in test profile");
        assertEquals("demo@textspeech.local", demoAuthProperties.getEmail());
        assertEquals("Demo User", demoAuthProperties.getName());
        assertNotNull(demoAuthProperties.getPasswordHash());
        assertTrue(demoAuthProperties.getPasswordHash().startsWith("$2a$"), "Password hash should be valid BCrypt format");
    }
}
