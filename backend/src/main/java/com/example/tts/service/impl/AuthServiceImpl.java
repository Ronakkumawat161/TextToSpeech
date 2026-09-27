package com.example.tts.service.impl;

import com.example.tts.dto.AuthResponse;
import com.example.tts.dto.LoginRequest;
import com.example.tts.dto.RegisterRequest;
import com.example.tts.dto.UserDto;
import com.example.tts.exception.InvalidCredentialsException;
import com.example.tts.exception.UserAlreadyExistsException;
import com.example.tts.model.Role;
import com.example.tts.model.User;
import com.example.tts.repository.UserRepository;
import com.example.tts.security.JwtTokenProvider;
import com.example.tts.security.UserPrincipal;
import com.example.tts.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of AuthService for managing user registration and authentication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // 1. Check for duplicate email
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.warn("Registration rejected: Email '{}' is already in use", normalizedEmail);
            throw new UserAlreadyExistsException(normalizedEmail);
        }

        // 2. Hash password with BCrypt (never store plaintext)
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 3. Create and persist User entity
        User user = User.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .password(hashedPassword)
                .role(Role.ROLE_USER)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user ID: {} with email '{}'", savedUser.getId(), savedUser.getEmail());

        // 4. Generate JWT for the new user
        UserPrincipal principal = new UserPrincipal(savedUser);
        String token = tokenProvider.generateToken(principal);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .expiresInMs(tokenProvider.getExpirationMs())
                .user(mapToDto(savedUser))
                .message("User registered successfully")
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        try {
            // Authenticate credentials via AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

            String token = tokenProvider.generateToken(principal);
            log.info("User logged in successfully: '{}'", normalizedEmail);

            return AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .expiresInMs(tokenProvider.getExpirationMs())
                    .user(mapToDto(principal.getUser()))
                    .message("Authentication successful")
                    .build();

        } catch (AuthenticationException ex) {
            log.warn("Failed login attempt for email '{}': {}", normalizedEmail, ex.getMessage());
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    @Override
    public UserDto getCurrentUserProfile() {
        User user = getCurrentUserEntity();
        if (user == null) {
            return null;
        }
        return mapToDto(user);
    }

    @Override
    public User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principalObj = authentication.getPrincipal();
        if (principalObj instanceof UserPrincipal principal) {
            return principal.getUser();
        }

        if (principalObj instanceof String email && !"anonymousUser".equals(email)) {
            return userRepository.findByEmailIgnoreCase(email).orElse(null);
        }

        return null;
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole() != null ? user.getRole().name() : "ROLE_USER")
                .createdAt(user.getCreatedAt())
                .build();
    }
}
