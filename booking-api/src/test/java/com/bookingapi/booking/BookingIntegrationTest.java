package com.bookingapi.booking;

import com.bookingapi.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class BookingIT extends AbstractIntegrationTest {

    private static final Instant START = Instant.now().plus(Duration.ofDays(30)).truncatedTo(ChronoUnit.HOURS);

    private static Instant hours(int offset) {
        return START.plus(Duration.ofHours(offset));
    }

    @Test
    void overlappingBookingsConflictButBackToBackOnesDoNot() throws Exception {
        String resourceId = createResource().get("id").asText();
        Session user = registerUser();

        JsonNode first = book(user.accessToken(), resourceId, hours(0), hours(1), 201);
        assertThat(first.get("data").get("status").asText()).isEqualTo("CONFIRMED");

        JsonNode conflict = book(user.accessToken(), resourceId,
                hours(0).plus(Duration.ofMinutes(30)), hours(1).plus(Duration.ofMinutes(30)), 409);
        assertThat(conflict.get("status").asInt()).isEqualTo(409);

        book(user.accessToken(), resourceId, hours(1), hours(2), 201);   // [start, end) is half-open
    }

    @Test
    void invalidTimeRangesAreRejected() throws Exception {
        String resourceId = createResource().get("id").asText();
        Session user = registerUser();

        book(user.accessToken(), resourceId, hours(2), hours(1), 400);                       // end before start
        book(user.accessToken(), resourceId, Instant.now().minusSeconds(3600), hours(1), 400); // start in the past
    }

    @Test
    void cancellingFreesTheSlot() throws Exception {
        String resourceId = createResource().get("id").asText();
        Session user = registerUser();

        String bookingId = book(user.accessToken(), resourceId, hours(0), hours(1), 201)
                .get("data").get("id").asText();

        JsonNode cancelled = patchJson("/api/v1/bookings/" + bookingId + "/cancel", user.accessToken(), 200);
        assertThat(cancelled.get("data").get("status").asText()).isEqualTo("CANCELLED");

        patchJson("/api/v1/bookings/" + bookingId + "/cancel", user.accessToken(), 409);   // already cancelled
        book(user.accessToken(), resourceId, hours(0), hours(1), 201);                      // slot is free again
    }

    @Test
    void usersOnlySeeTheirOwnBookingsAdminsSeeAll() throws Exception {
        String resourceId = createResource().get("id").asText();
        Session owner = registerUser();
        Session other = registerUser();

        String bookingId = book(owner.accessToken(), resourceId, hours(0), hours(1), 201)
                .get("data").get("id").asText();
        String url = "/api/v1/bookings/" + bookingId;

        getJson(url, owner.accessToken(), 200);
        getJson(url, adminToken(), 200);
        getJson(url, other.accessToken(), 404);                       // not "403": ids can't be probed
        patchJson(url + "/cancel", other.accessToken(), 404);

        JsonNode othersList = getJson("/api/v1/bookings", other.accessToken(), 200);
        assertThat(othersList.get("data").get("totalElements").asLong()).isZero();
    }

    @Test
    void listsArePaginatedSortedAndFiltered() throws Exception {
        String resourceId = createResource().get("id").asText();
        Session user = registerUser();
        book(user.accessToken(), resourceId, hours(0), hours(1), 201);
        book(user.accessToken(), resourceId, hours(2), hours(3), 201);

        JsonNode page = send(MockMvcRequestBuilders.get("/api/v1/bookings")
                        .param("page", "0").param("size", "1").param("sort", "startTime,asc"),
                user.accessToken(), null, 200).get("data");

        assertThat(page.get("content")).hasSize(1);
        assertThat(page.get("totalElements").asLong()).isEqualTo(2);
        assertThat(page.get("totalPages").asInt()).isEqualTo(2);
        assertThat(page.get("last").asBoolean()).isFalse();
        assertThat(Instant.parse(page.get("content").get(0).get("startTime").asText())).isEqualTo(hours(0));

        JsonNode filtered = send(MockMvcRequestBuilders.get("/api/v1/bookings")
                        .param("status", "CANCELLED"),
                user.accessToken(), null, 200).get("data");
        assertThat(filtered.get("totalElements").asLong()).isZero();
    }

    @Test
    void unsupportedSortPropertiesAreRejected() throws Exception {
        Session user = registerUser();

        send(MockMvcRequestBuilders.get("/api/v1/bookings").param("sort", "user.password"),
                user.accessToken(), null, 400);
    }
}