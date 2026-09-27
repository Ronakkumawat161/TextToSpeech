package com.example.tts.security;

import com.example.tts.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory Token Bucket Rate Limiter.
 * Protects synthesis endpoints (/api/tts) from abuse and resource exhaustion.
 */
@Slf4j
@Service
public class RateLimiterService {

    private final SecurityProperties securityProperties;
    private final ConcurrentMap<String, TokenBucket> ipBuckets = new ConcurrentHashMap<>();

    public RateLimiterService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * Represents a rate limit check result
     */
    public record RateLimitResult(boolean allowed, int retryAfterSeconds) {}

    /**
     * Check if a request from the given client IP should be allowed.
     *
     * @param clientIp Client IP address
     * @return RateLimitResult indicating if allowed and seconds to wait before retry
     */
    public RateLimitResult checkRateLimit(String clientIp) {
        if (!securityProperties.getRateLimit().isEnabled()) {
            return new RateLimitResult(true, 0);
        }

        int rpm = Math.max(1, securityProperties.getRateLimit().getRequestsPerMinute());
        int capacity = Math.max(1, securityProperties.getRateLimit().getBurstCapacity());

        // Clean up idle buckets if memory usage grows
        if (ipBuckets.size() > 5000) {
            cleanupIdleBuckets();
        }

        TokenBucket bucket = ipBuckets.computeIfAbsent(clientIp, k -> new TokenBucket(capacity, rpm));
        return bucket.tryConsume();
    }

    /**
     * Clear all rate limiting records (useful for testing)
     */
    public void reset() {
        ipBuckets.clear();
    }

    private void cleanupIdleBuckets() {
        long now = System.currentTimeMillis();
        // Remove buckets that have been idle for more than 10 minutes
        ipBuckets.entrySet().removeIf(entry -> (now - entry.getValue().lastRefillTime) > 600_000L);
    }

    /**
     * Internal Token Bucket state per client IP
     */
    static class TokenBucket {
        private final int capacity;
        private final double refillRatePerSecond;
        private double tokens;
        private long lastRefillTime;

        public TokenBucket(int capacity, int requestsPerMinute) {
            this.capacity = capacity;
            this.refillRatePerSecond = (double) requestsPerMinute / 60.0;
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized RateLimitResult tryConsume() {
            refill();

            if (tokens >= 1.0) {
                tokens -= 1.0;
                return new RateLimitResult(true, 0);
            }

            // Calculate wait time in seconds until at least 1 token is available
            double neededTokens = 1.0 - tokens;
            int waitSeconds = (int) Math.ceil(neededTokens / refillRatePerSecond);
            return new RateLimitResult(false, Math.max(1, waitSeconds));
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double elapsedSeconds = (now - lastRefillTime) / 1000.0;
            if (elapsedSeconds > 0) {
                double tokensToAdd = elapsedSeconds * refillRatePerSecond;
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillTime = now;
            }
        }
    }
}
