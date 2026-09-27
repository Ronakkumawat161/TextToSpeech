package com.example.tts.service.impl;

import com.example.tts.config.DemoAuthProperties;
import com.example.tts.dto.AuthResponse;
import com.example.tts.dto.LoginRequest;
import com.example.tts.dto.RegisterRequest;
import com.example.tts.dto.UserDto;
import com.example.tts.exception.InvalidCredentialsException;
import com.example.tts.exception.ServiceUnavailableException;
import com.example.tts.exception.UserAlreadyExistsException;
import com.example.tts.model.Role;
import com.example.tts.model.User;
import com.example.tts.repository.UserRepository;
import com.example.tts.security.JwtTokenProvider;
import com.example.tts.security.UserPrincipal;
import com.example.tts.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Includes development-only fallback for predefined demo account when PostgreSQL is unavailable.
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final DemoAuthProperties demoAuthProperties;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            AuthenticationManager authenticationManager) {
        this(userRepository, passwordEncoder, tokenProvider, authenticationManager, new DemoAuthProperties());
    }

    @Autowired
    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            AuthenticationManager authenticationManager,
            DemoAuthProperties demoAuthProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.demoAuthProperties = demoAuthProperties != null ? demoAuthProperties : new DemoAuthProperties();
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // 1. Prevent registering with the reserved demo account email
        if (demoAuthProperties.isEnabled() && normalizedEmail.equalsIgnoreCase(demoAuthProperties.getEmail())) {
            log.warn("Registration rejected: Demo email '{}' is reserved", normalizedEmail);
            throw new UserAlreadyExistsException("Email '" + normalizedEmail + "' is reserved for the demo account. Please sign in directly.");
        }

        // 2. Check for duplicate email in database
        try {
            if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                log.warn("Registration rejected: Email '{}' is already in use", normalizedEmail);
                throw new UserAlreadyExistsException(normalizedEmail);
            }
        } catch (UserAlreadyExistsException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Database connection failure during registration duplicate check: {}", ex.getMessage());
            throw new ServiceUnavailableException("Database service is temporarily unavailable. For local testing, use the demo account: " + demoAuthProperties.getEmail());
        }

        // 3. Hash password with BCrypt (never store plaintext)
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 4. Create and persist User entity
        User user = User.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .password(hashedPassword)
                .role(Role.ROLE_USER)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            User savedUser = userRepository.save(user);
            log.info("Registered new user ID: {} with email '{}'", savedUser.getId(), savedUser.getEmail());

            // Generate JWT for the new user
            UserPrincipal principal = new UserPrincipal(savedUser);
            String token = tokenProvider.generateToken(principal);

            return AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .expiresInMs(tokenProvider.getExpirationMs())
                    .user(mapToDto(savedUser))
                    .message("User registered successfully")
                    .build();
        } catch (Exception ex) {
            log.error("Database persistence failed during user registration: {}", ex.getMessage());
            throw new ServiceUnavailableException("Database service is temporarily unavailable. For local testing, use the demo account: " + demoAuthProperties.getEmail());
        }
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String rawPassword = request.getPassword();

        // 1. Check development/demo authentication fallback
        if (demoAuthProperties.isEnabled() && normalizedEmail.equalsIgnoreCase(demoAuthProperties.getEmail())) {
            log.info("Processing login request for predefined demo account '{}'", normalizedEmail);

            // Verify password using BCrypt against the configured hash (never plaintext)
            if (!passwordEncoder.matches(rawPassword, demoAuthProperties.getPasswordHash())) {
                log.warn("Failed demo login attempt for email '{}': Invalid password", normalizedEmail);
                throw new InvalidCredentialsException("Invalid email or password");
            }

            // Find or synthesize demo user entity
            User demoUser = resolveDemoUser();

            UserPrincipal principal = new UserPrincipal(demoUser);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String token = tokenProvider.generateToken(principal);
            log.info("Demo user authenticated successfully: '{}'", normalizedEmail);

            return AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .expiresInMs(tokenProvider.getExpirationMs())
                    .user(mapToDto(demoUser))
                    .message("Authentication successful")
                    .build();
        }

        // 2. Normal production / database authentication flow
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, rawPassword)
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
            if (demoAuthProperties.isEnabled() && email.equalsIgnoreCase(demoAuthProperties.getEmail())) {
                return resolveDemoUser();
            }
            try {
                return userRepository.findByEmailIgnoreCase(email).orElse(null);
            } catch (Exception ex) {
                log.debug("Database error fetching user by email '{}': {}", email, ex.getMessage());
                return null;
            }
        }

        return null;
    }

    /**
     * Resolves the demo user: fetches/seeds in PostgreSQL if database is running;
     * otherwise falls back to an in-memory representation.
     */
    private User resolveDemoUser() {
        try {
            var existingUser = userRepository.findByEmailIgnoreCase(demoAuthProperties.getEmail());
            if (existingUser.isPresent()) {
                return existingUser.get();
            }
            // If PostgreSQL is available, seed demo user
            User seededDemo = User.builder()
                    .name(demoAuthProperties.getName())
                    .email(demoAuthProperties.getEmail().toLowerCase())
                    .password(demoAuthProperties.getPasswordHash())
                    .role(Role.ROLE_USER)
                    .createdAt(LocalDateTime.now())
                    .build();
            return userRepository.save(seededDemo);
        } catch (Exception ex) {
            log.info("Database unavailable for demo user persistence; using in-memory demo entity: {}", ex.getMessage());
        }

        // Database unavailable: return in-memory demo User entity
        return User.builder()
                .id(-1L)
                .name(demoAuthProperties.getName())
                .email(demoAuthProperties.getEmail().toLowerCase())
                .password(demoAuthProperties.getPasswordHash())
                .role(Role.ROLE_USER)
                .createdAt(LocalDateTime.now())
                .build();
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
