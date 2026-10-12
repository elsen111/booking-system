package com.bookingapi.domain.resource.dto;

import java.time.Instant;
import java.util.UUID;

public record ResourceResponse(
        UUID id,
        String name,
        String description,
        int capacity,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}