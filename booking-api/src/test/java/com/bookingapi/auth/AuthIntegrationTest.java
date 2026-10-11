package com.bookingapi.auth;

import com.bookingapi.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void refreshRotatesTheTokenAndReuseRevokesTheWholeSession() throws Exception {
        Session session = registerUser();

        JsonNode rotated = postJson("/api/v1/auth/refresh", null,
                Map.of("refreshToken", session.refreshToken()), 200).get("data");
        String newRefreshToken = rotated.get("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(session.refreshToken());

        // Replaying the old token looks like theft...
        postJson("/api/v1/auth/refresh", null, Map.of("refreshToken", session.refreshToken()), 401);
        // ...so the rotated token must be dead as well
        postJson("/api/v1/auth/refresh", null, Map.of("refreshToken", newRefreshToken), 401);
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        Session session = registerUser();

        postJson("/api/v1/auth/logout", null, Map.of("refreshToken", session.refreshToken()), 200);
        postJson("/api/v1/auth/refresh", null, Map.of("refreshToken", session.refreshToken()), 401);
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        Session session = registerUser();

        postJson("/api/v1/auth/login", null,
                Map.of("email", session.email(), "password", "wrong-password"), 401);
    }

    @Test
    void registeringTheSameEmailTwiceConflicts() throws Exception {
        Session session = registerUser();

        postJson("/api/v1/auth/register", null,
                Map.of("email", session.email(), "password", "Passw0rd!123", "fullName", "Again"), 409);
    }

    @Test
    void invalidRegistrationReturnsFieldErrors() throws Exception {
        JsonNode error = postJson("/api/v1/auth/register", null,
                Map.of("email", "not-an-email", "password", "short", "fullName", "X"), 400);

        assertThat(error.get("success").asBoolean()).isFalse();
        assertThat(error.get("fieldErrors").has("email")).isTrue();
        assertThat(error.get("fieldErrors").has("password")).isTrue();
    }
}