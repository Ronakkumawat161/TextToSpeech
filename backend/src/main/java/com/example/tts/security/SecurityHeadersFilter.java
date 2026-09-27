package com.example.tts.security;

import com.example.tts.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter applying essential modern HTTP security headers to all HTTP responses.
 * Protects against XSS, clickjacking, MIME confusion, and referrer leakage.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (securityProperties.getHeaders().isEnabled()) {
            // Prevent MIME-sniffing
            response.setHeader("X-Content-Type-Options", securityProperties.getHeaders().getContentTypeOptions());

            // Prevent framing / clickjacking
            response.setHeader("X-Frame-Options", securityProperties.getHeaders().getFrameOptions());

            // Enable browser XSS protection
            response.setHeader("X-XSS-Protection", securityProperties.getHeaders().getXssProtection());

            // Control referrer information leakage
            response.setHeader("Referrer-Policy", securityProperties.getHeaders().getReferrerPolicy());

            // Disable unwanted browser sensors/features
            response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        }

        filterChain.doFilter(request, response);
    }
}
