package com.example.tts.security;

import com.example.tts.config.DemoAuthProperties;
import com.example.tts.model.Role;
import com.example.tts.model.User;
import com.example.tts.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Custom UserDetailsService loading UserPrincipal by email address.
 * Supports fallback demo user principal in development mode when database is unavailable.
 */
@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final DemoAuthProperties demoAuthProperties;

    public CustomUserDetailsService(UserRepository userRepository) {
        this(userRepository, new DemoAuthProperties());
    }

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository, DemoAuthProperties demoAuthProperties) {
        this.userRepository = userRepository;
        this.demoAuthProperties = demoAuthProperties != null ? demoAuthProperties : new DemoAuthProperties();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";

        // Development Demo Account Fallback
        if (demoAuthProperties.isEnabled() && normalizedEmail.equalsIgnoreCase(demoAuthProperties.getEmail())) {
            try {
                User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
                if (user != null) {
                    return new UserPrincipal(user);
                }
            } catch (Exception ex) {
                log.debug("Database unavailable when loading demo user '{}': {}", normalizedEmail, ex.getMessage());
            }

            // In-memory demo user principal for local development
            User demoUser = User.builder()
                    .id(-1L)
                    .name(demoAuthProperties.getName())
                    .email(demoAuthProperties.getEmail().toLowerCase())
                    .password(demoAuthProperties.getPasswordHash())
                    .role(Role.ROLE_USER)
                    .createdAt(LocalDateTime.now())
                    .build();
            return new UserPrincipal(demoUser);
        }

        // Standard user lookup
        try {
            User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                    .orElseThrow(() -> {
                        log.warn("Authentication failed: User with email '{}' not found", email);
                        return new UsernameNotFoundException("User not found with email: " + email);
                    });

            return new UserPrincipal(user);
        } catch (UsernameNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Database connection failure while loading user '{}': {}", email, ex.getMessage());
            throw new UsernameNotFoundException("User service temporarily unavailable: " + email, ex);
        }
    }
}
