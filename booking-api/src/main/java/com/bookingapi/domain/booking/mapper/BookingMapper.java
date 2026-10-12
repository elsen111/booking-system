package com.bookingapi.domain.booking.mapper;

import com.bookingapi.domain.booking.entity.Booking;
import com.bookingapi.domain.booking.dto.BookingResponse;
import com.bookingapi.domain.booking.dto.CreateBookingRequest;
import com.bookingapi.domain.resource.entity.Resource;
import com.bookingapi.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface BookingMapper {

    @Mapping(target = "resourceId", source = "resource.id")
    @Mapping(target = "resourceName", source = "resource.name")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    BookingResponse toResponse(Booking booking);

    @Mapping(target = "resource", source = "resource")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "startTime", source = "request.startTime")
    @Mapping(target = "endTime", source = "request.endTime")
    @Mapping(target = "status", constant = "CONFIRMED")
    Booking toEntity(CreateBookingRequest request, Resource resource, User user);
}