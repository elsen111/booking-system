package com.bookingapi.booking.dto;

import com.bookingapi.booking.domain.BookingStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.UUID;

public record BookingFilter(
        UUID userId,
        UUID resourceId,
        BookingStatus status,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
) {

    public BookingFilter forUser(UUID userId) {
        return new BookingFilter(userId, resourceId, status, from, to);
    }
}