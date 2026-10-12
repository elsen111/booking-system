package com.bookingapi.domain.booking.service;

import com.bookingapi.domain.booking.dto.BookingFilter;
import com.bookingapi.domain.booking.dto.BookingResponse;
import com.bookingapi.domain.booking.dto.CreateBookingRequest;
import com.bookingapi.common.dto.PageResponse;
import com.bookingapi.security.AuthenticatedUser;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BookingService {

    PageResponse<BookingResponse> search(BookingFilter filter, Pageable pageable, AuthenticatedUser currentUser);

    BookingResponse getById(UUID id, AuthenticatedUser currentUser);

    BookingResponse create(CreateBookingRequest request, AuthenticatedUser currentUser);

    BookingResponse cancel(UUID id, AuthenticatedUser currentUser);
}