package com.bookingapi.booking.dto;

import com.bookingapi.booking.domain.BookingStatus;

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