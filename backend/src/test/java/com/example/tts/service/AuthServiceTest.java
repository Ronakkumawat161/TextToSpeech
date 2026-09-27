package com.example.tts.service;

import com.example.tts.dto.AuthResponse;
import com.example.tts.dto.LoginRequest;
import com.example.tts.dto.RegisterRequest;
import com.example.tts.exception.InvalidCredentialsException;
import com.example.tts.exception.UserAlreadyExistsException;
import com.example.tts.model.Role;
import com.example.tts.model.User;
import com.example.tts.repository.UserRepository;
import com.example.tts.security.JwtTokenProvider;
import com.example.tts.security.UserPrincipal;
import com.example.tts.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, tokenProvider, authenticationManager);
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("secret123")
                .build();

        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$hashedPassword");

        User savedUser = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .password("$2a$10$hashedPassword")
                .role(Role.ROLE_USER)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateToken(any(UserPrincipal.class))).thenReturn("mock-jwt-token");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("Bearer", response.getType());
        assertEquals("alice@example.com", response.getUser().getEmail());
        assertEquals("Alice", response.getUser().getName());
        verify(passwordEncoder, times(1)).encode("secret123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateEmailThrowsUserAlreadyExistsException() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("secret123")
                .build();

        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testLoginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .email("alice@example.com")
                .password("secret123")
                .build();

        User user = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .password("$2a$10$hashedPassword")
                .role(Role.ROLE_USER)
                .build();

        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(tokenProvider.generateToken(principal)).thenReturn("valid-jwt-token");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("valid-jwt-token", response.getToken());
        assertEquals("alice@example.com", response.getUser().getEmail());
    }

    @Test
    void testLoginInvalidCredentialsThrowsInvalidCredentialsException() {
        LoginRequest request = LoginRequest.builder()
                .email("alice@example.com")
                .password("wrongpassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }
}
