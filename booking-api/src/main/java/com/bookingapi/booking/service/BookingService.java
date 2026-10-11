package com.bookingapi.booking.service;

import com.bookingapi.booking.dto.BookingFilter;
import com.bookingapi.booking.dto.BookingResponse;
import com.bookingapi.booking.dto.CreateBookingRequest;
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