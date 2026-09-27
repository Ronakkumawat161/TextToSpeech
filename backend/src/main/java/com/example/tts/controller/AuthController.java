package com.example.tts.controller;

import com.example.tts.dto.AuthResponse;
import com.example.tts.dto.LoginRequest;
import com.example.tts.dto.RegisterRequest;
import com.example.tts.dto.UserDto;
import com.example.tts.exception.UnauthorizedException;
import com.example.tts.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for User Authentication and Registration.
 * 
 * Endpoints:
 * - POST /api/auth/register : Register a new user
 * - POST /api/auth/login    : Authenticate and obtain JWT
 * - GET  /api/auth/me       : Get currently authenticated user profile
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user account.
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Received registration request for email: '{}'", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate user with credentials and return JWT token.
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Received login request for email: '{}'", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve the currently authenticated user's profile.
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        UserDto userDto = authService.getCurrentUserProfile();
        if (userDto == null) {
            throw new UnauthorizedException("No authenticated user session found");
        }
        return ResponseEntity.ok(userDto);
    }
}
