package com.example.tts.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for development-only demo authentication fallback.
 * Allows local testing without requiring a running PostgreSQL instance.
 * Strictly disabled in production.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.demo-auth")
public class DemoAuthProperties {

    /**
     * Whether development demo authentication fallback is enabled.
     * Default: false (enabled only in dev profile).
     */
    private boolean enabled = false;

    /**
     * Dedicated demo user email address.
     */
    private String email = "demo@textspeech.local";

    /**
     * Predefined display name for the demo account.
     */
    private String name = "Demo User";

    /**
     * BCrypt password hash for Demo@12345.
     * Plaintext passwords are NEVER stored in source code or database.
     */
    private String passwordHash = "$2a$10$FycIICn.qpMIrA1F7Oy5v.LoiLJ0aBw8NBrsqvE8kshqLP3ZNk0Qm";
}
