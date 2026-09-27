package com.example.tts.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Security configuration properties for rate limiting, security headers, and origin controls.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private RateLimit rateLimit = new RateLimit();
    private Headers headers = new Headers();

    @Data
    public static class RateLimit {
        /**
         * Whether IP-based rate limiting is enabled
         */
        private boolean enabled = true;

        /**
         * Allowed requests per minute per IP address for /api/tts
         */
        private int requestsPerMinute = 30;

        /**
         * Allowed immediate burst requests
         */
        private int burstCapacity = 10;
    }

    @Data
    public static class Headers {
        /**
         * Whether HTTP security headers are enabled
         */
        private boolean enabled = true;

        /**
         * X-Frame-Options value (e.g. DENY, SAMEORIGIN)
         */
        private String frameOptions = "DENY";

        /**
         * X-Content-Type-Options value (nosniff)
         */
        private String contentTypeOptions = "nosniff";

        /**
         * X-XSS-Protection value (1; mode=block)
         */
        private String xssProtection = "1; mode=block";

        /**
         * Referrer-Policy value
         */
        private String referrerPolicy = "strict-origin-when-cross-origin";
    }
}
