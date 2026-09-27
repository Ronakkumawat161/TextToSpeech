package com.example.tts.controller;

import com.example.tts.dto.AuthResponse;
import com.example.tts.dto.LoginRequest;
import com.example.tts.dto.RegisterRequest;
import com.example.tts.dto.UserDto;
import com.example.tts.exception.InvalidCredentialsException;
import com.example.tts.exception.UserAlreadyExistsException;
import com.example.tts.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void testRegisterSuccessReturns201Created() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .password("password123")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt-token-12345")
                .type("Bearer")
                .expiresInMs(86400000L)
                .user(UserDto.builder()
                        .id(1L)
                        .name("Jane Doe")
                        .email("jane@example.com")
                        .role("ROLE_USER")
                        .createdAt(LocalDateTime.now())
                        .build())
                .message("User registered successfully")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token-12345"))
                .andExpect(jsonPath("$.user.email").value("jane@example.com"))
                .andExpect(jsonPath("$.user.name").value("Jane Doe"));
    }

    @Test
    void testRegisterDuplicateEmailReturns409Conflict() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Jane Doe")
                .email("duplicate@example.com")
                .password("password123")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UserAlreadyExistsException("duplicate@example.com"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("USER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.error.message", containsString("already exists")));
    }

    @Test
    void testRegisterInvalidEmailReturns400BadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Jane Doe")
                .email("not-an-email")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void testLoginSuccessReturns200OkWithToken() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("jane@example.com")
                .password("password123")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt-login-token-67890")
                .type("Bearer")
                .expiresInMs(86400000L)
                .user(UserDto.builder()
                        .id(1L)
                        .name("Jane Doe")
                        .email("jane@example.com")
                        .role("ROLE_USER")
                        .build())
                .message("Authentication successful")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-login-token-67890"))
                .andExpect(jsonPath("$.user.email").value("jane@example.com"));
    }

    @Test
    void testLoginInvalidCredentialsReturns401Unauthorized() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("jane@example.com")
                .password("wrongpassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message", containsString("Invalid email or password")));
    }

    @Test
    @WithMockUser(username = "jane@example.com")
    void testGetCurrentUserWithAuthReturns200Ok() throws Exception {
        UserDto userDto = UserDto.builder()
                .id(1L)
                .name("Jane Doe")
                .email("jane@example.com")
                .role("ROLE_USER")
                .build();

        when(authService.getCurrentUserProfile()).thenReturn(userDto);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.name").value("Jane Doe"));
    }
}
