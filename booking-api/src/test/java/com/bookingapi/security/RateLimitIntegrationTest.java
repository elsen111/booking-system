package com.bookingapi.security;

import com.bookingapi.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

@TestPropertySource(properties = "app.rate-limit.auth-per-minute=3")
class RateLimitIT extends AbstractIntegrationTest {

    @Test
    void authEndpointsAreThrottledPerClient() throws Exception {
        Map<String, String> wrongLogin = Map.of("email", "nobody@test.com", "password", "wrong-password");

        for (int i = 0; i < 3; i++) {
            postJson("/api/v1/auth/login", null, wrongLogin, 401);
        }
        postJson("/api/v1/auth/login", null, wrongLogin, 429);
    }
}