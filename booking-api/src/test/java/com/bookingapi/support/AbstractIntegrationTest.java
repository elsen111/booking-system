package com.bookingapi.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@example.com";       // bootstrap admin (default config)
    protected static final String ADMIN_PASSWORD = "Admin12345!";

    // One container shared by all test classes
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected record Session(String email, String accessToken, String refreshToken) {
    }

    // ---- HTTP helpers: send the request, assert the status, return the parsed JSON body ----

    protected JsonNode send(MockHttpServletRequestBuilder request, String token, Object body, int expectedStatus)
            throws Exception {
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        String content = mvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }

    protected JsonNode getJson(String url, String token, int expectedStatus) throws Exception {
        return send(MockMvcRequestBuilders.get(url), token, null, expectedStatus);
    }

    protected JsonNode postJson(String url, String token, Object body, int expectedStatus) throws Exception {
        return send(MockMvcRequestBuilders.post(url), token, body, expectedStatus);
    }

    protected JsonNode putJson(String url, String token, Object body, int expectedStatus) throws Exception {
        return send(MockMvcRequestBuilders.put(url), token, body, expectedStatus);
    }

    protected JsonNode patchJson(String url, String token, int expectedStatus) throws Exception {
        return send(MockMvcRequestBuilders.patch(url), token, null, expectedStatus);
    }

    // ---- Domain helpers ----

    protected Session registerUser() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.com";
        JsonNode data = postJson("/api/v1/auth/register", null,
                Map.of("email", email, "password", "Passw0rd!123", "fullName", "Test User"), 201).get("data");
        return new Session(email, data.get("accessToken").asText(), data.get("refreshToken").asText());
    }

    protected String adminToken() throws Exception {
        return postJson("/api/v1/auth/login", null,
                Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD), 200)
                .get("data").get("accessToken").asText();
    }

    /** Creates an active resource with a unique name and returns its response data. */
    protected JsonNode createResource() throws Exception {
        return postJson("/api/v1/resources", adminToken(),
                Map.of("name", "Room-" + UUID.randomUUID(), "capacity", 6), 201).get("data");
    }

    protected JsonNode book(String token, String resourceId, Instant start, Instant end, int expectedStatus)
            throws Exception {
        return postJson("/api/v1/bookings", token,
                Map.of("resourceId", resourceId, "startTime", start, "endTime", end), expectedStatus);
    }
}