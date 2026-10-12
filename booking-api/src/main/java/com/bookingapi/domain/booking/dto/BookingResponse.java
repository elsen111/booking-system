package com.bookingapi.domain.booking.dto;

import com.bookingapi.domain.booking.entity.BookingStatus;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID resourceId,
        String resourceName,
        UUID userId,
        String userEmail,
        Instant startTime,
        Instant endTime,
        BookingStatus status,
        Instant createdAt
) {
}