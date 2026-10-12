package com.bookingapi.domain.booking.controller;

import com.bookingapi.domain.booking.dto.BookingFilter;
import com.bookingapi.domain.booking.dto.BookingResponse;
import com.bookingapi.domain.booking.dto.CreateBookingRequest;
import com.bookingapi.domain.booking.service.BookingService;
import com.bookingapi.common.dto.ApiResponse;
import com.bookingapi.common.dto.PageResponse;
import com.bookingapi.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Bookings")
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "List bookings (paginated, filterable). Users see their own, admins see all.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> search(
            @ParameterObject BookingFilter filter,
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.ok(bookingService.search(filter, pageable, currentUser)).toResponseEntity();
    }

    @Operation(summary = "Get a booking by id (owner or admin)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getById(@PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.ok(bookingService.getById(id, currentUser)).toResponseEntity();
    }

    @Operation(summary = "Book a resource for a time range")
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> create(@Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.created("Booking confirmed", bookingService.create(request, currentUser)).toResponseEntity();
    }

    @Operation(summary = "Cancel a booking (owner or admin)")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancel(@PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.ok("Booking cancelled", bookingService.cancel(id, currentUser)).toResponseEntity();
    }
}