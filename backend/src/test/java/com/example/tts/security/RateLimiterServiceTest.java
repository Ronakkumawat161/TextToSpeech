package com.example.tts.security;

import com.example.tts.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private SecurityProperties properties;
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        properties = new SecurityProperties();
        properties.getRateLimit().setEnabled(true);
        properties.getRateLimit().setRequestsPerMinute(60);
        properties.getRateLimit().setBurstCapacity(3); // Small burst capacity for testing
        rateLimiterService = new RateLimiterService(properties);
    }

    @Test
    void testRequestsWithinBurstCapacityAreAllowed() {
        String clientIp = "192.168.1.100";

        // Consume 3 burst tokens
        assertTrue(rateLimiterService.checkRateLimit(clientIp).allowed());
        assertTrue(rateLimiterService.checkRateLimit(clientIp).allowed());
        assertTrue(rateLimiterService.checkRateLimit(clientIp).allowed());
    }

    @Test
    void testExcessiveRequestsAreRejectedWith429RetryAfter() {
        String clientIp = "192.168.1.101";

        // Exhaust burst capacity (3)
        assertTrue(rateLimiterService.checkRateLimit(clientIp).allowed());
        assertTrue(rateLimiterService.checkRateLimit(clientIp).allowed());
        assertTrue(rateLimiterService.checkRateLimit(clientIp).allowed());

        // 4th request must be rejected
        RateLimiterService.RateLimitResult result = rateLimiterService.checkRateLimit(clientIp);
        assertFalse(result.allowed());
        assertTrue(result.retryAfterSeconds() >= 1);
    }

    @Test
    void testDifferentIpsHaveIndependentBuckets() {
        String ip1 = "10.0.0.1";
        String ip2 = "10.0.0.2";

        // Exhaust IP 1
        assertTrue(rateLimiterService.checkRateLimit(ip1).allowed());
        assertTrue(rateLimiterService.checkRateLimit(ip1).allowed());
        assertTrue(rateLimiterService.checkRateLimit(ip1).allowed());
        assertFalse(rateLimiterService.checkRateLimit(ip1).allowed());

        // IP 2 is fresh and still allowed
        assertTrue(rateLimiterService.checkRateLimit(ip2).allowed());
    }

    @Test
    void testRateLimitDisabledAllowsAllRequests() {
        properties.getRateLimit().setEnabled(false);
        String clientIp = "192.168.1.102";

        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimiterService.checkRateLimit(clientIp).allowed());
        }
    }
}
