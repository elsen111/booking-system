package com.bookingapi.resource;

import com.bookingapi.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceAccessIT extends AbstractIntegrationTest {

    @Test
    void requestsWithoutATokenAreUnauthorized() throws Exception {
        getJson("/api/v1/resources", null, 401);
    }

    @Test
    void onlyAdminsCanCreateResources() throws Exception {
        Session user = registerUser();
        Map<String, Object> body = Map.of("name", "Hall-" + user.email(), "capacity", 20);

        postJson("/api/v1/resources", user.accessToken(), body, 403);

        JsonNode created = postJson("/api/v1/resources", adminToken(), body, 201);
        assertThat(created.get("status").asInt()).isEqualTo(201);
        assertThat(created.get("data").get("active").asBoolean()).isTrue();
    }

    @Test
    void usersCanReadResources() throws Exception {
        Session user = registerUser();
        String id = createResource().get("id").asText();

        getJson("/api/v1/resources/" + id, user.accessToken(), 200);
        getJson("/api/v1/resources", user.accessToken(), 200);
    }

    @Test
    void deactivatedResourcesAreHiddenFromUsersButNotFromAdmins() throws Exception {
        Session user = registerUser();
        String admin = adminToken();
        JsonNode resource = createResource();
        String id = resource.get("id").asText();

        putJson("/api/v1/resources/" + id, admin,
                Map.of("name", resource.get("name").asText(), "capacity", 6, "active", false), 200);

        getJson("/api/v1/resources/" + id, user.accessToken(), 404);
        getJson("/api/v1/resources/" + id, admin, 200);
    }
}