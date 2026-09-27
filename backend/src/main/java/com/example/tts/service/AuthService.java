package com.example.tts.service;

import com.example.tts.dto.AuthResponse;
import com.example.tts.dto.LoginRequest;
import com.example.tts.dto.RegisterRequest;
import com.example.tts.dto.UserDto;
import com.example.tts.model.User;

/**
 * Service interface for user registration and authentication.
 */
public interface AuthService {

    /**
     * Register a new user account with hashed password and return JWT
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticate an existing user with email and password and return JWT
     */
    AuthResponse login(LoginRequest request);

    /**
     * Retrieve the currently authenticated user profile
     */
    UserDto getCurrentUserProfile();

    /**
     * Retrieve the currently authenticated User entity (or null if anonymous)
     */
    User getCurrentUserEntity();
}
