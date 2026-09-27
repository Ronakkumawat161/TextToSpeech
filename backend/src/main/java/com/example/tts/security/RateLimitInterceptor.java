package com.example.tts.security;

import com.example.tts.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rate Limiting Interceptor for /api/tts endpoint.
 * Protects synthesis API against request flooding and denial of service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Skip rate limiting for OPTIONS preflight checks
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Only enforce rate limiting on synthesis and AI enhancement POST operations
        if ("POST".equalsIgnoreCase(request.getMethod()) &&
                (request.getRequestURI().endsWith("/api/tts") || request.getRequestURI().contains("/api/ai/enhance"))) {
            String clientIp = extractClientIp(request);
            RateLimiterService.RateLimitResult result = rateLimiterService.checkRateLimit(clientIp);

            if (!result.allowed()) {
                log.warn("Rate limit exceeded for client IP: {}", clientIp);
                response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
                throw new RateLimitExceededException(
                        "Rate limit exceeded. Please wait " + result.retryAfterSeconds() + " seconds before retrying."
                );
            }
        }

        return true;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
