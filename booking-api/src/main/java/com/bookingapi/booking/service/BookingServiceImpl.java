package com.bookingapi.booking.service;

import com.bookingapi.booking.domain.Booking;
import com.bookingapi.booking.domain.BookingStatus;
import com.bookingapi.booking.dto.BookingFilter;
import com.bookingapi.booking.dto.BookingResponse;
import com.bookingapi.booking.dto.CreateBookingRequest;
import com.bookingapi.booking.mapper.BookingMapper;
import com.bookingapi.booking.repository.BookingRepository;
import com.bookingapi.booking.repository.BookingSpecifications;
import com.bookingapi.common.exception.BadRequestException;
import com.bookingapi.common.exception.ConflictException;
import com.bookingapi.common.exception.NotFoundException;
import com.bookingapi.common.exception.UnauthorizedException;
import com.bookingapi.common.pagination.PageableSanitizer;
import com.bookingapi.common.dto.PageResponse;
import com.bookingapi.resource.domain.Resource;
import com.bookingapi.resource.repository.ResourceRepository;
import com.bookingapi.security.AuthenticatedUser;
import com.bookingapi.user.domain.User;
import com.bookingapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private static final Set<String> SORTABLE = Set.of("startTime", "endTime", "createdAt", "status");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "startTime");
    private static final String SLOT_TAKEN = "The resource is already booked for the selected time";

    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    @Override
    public PageResponse<BookingResponse> search(BookingFilter filter, Pageable pageable, AuthenticatedUser currentUser) {
        BookingFilter effective = currentUser.isAdmin() ? filter : filter.forUser(currentUser.id());
        Pageable safePageable = PageableSanitizer.sanitize(pageable, SORTABLE, DEFAULT_SORT);

        Page<Booking> page = bookingRepository.findAll(BookingSpecifications.from(effective), safePageable);
        return PageResponse.from(page.map(bookingMapper::toResponse));
    }

    @Override
    public BookingResponse getById(UUID id, AuthenticatedUser currentUser) {
        return bookingMapper.toResponse(findAccessible(id, currentUser));
    }

    @Override
    @Transactional
    public BookingResponse create(CreateBookingRequest request, AuthenticatedUser currentUser) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("endTime must be after startTime");
        }

        Resource resource = resourceRepository.findById(request.resourceId())
                .filter(Resource::isActive)
                .orElseThrow(() -> new NotFoundException("Resource not found"));
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));

        if (bookingRepository.existsOverlapping(resource.getId(), request.startTime(), request.endTime())) {
            throw new ConflictException(SLOT_TAKEN);
        }

        Booking booking = bookingMapper.toEntity(request, resource, user);
        try {
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException e) {
            // Two requests passed the check simultaneously and the constraint rejected the second one
            throw new ConflictException(SLOT_TAKEN);
        }
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse cancel(UUID id, AuthenticatedUser currentUser) {
        Booking booking = findAccessible(id, currentUser);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("Booking is already cancelled");
        }
        if (!booking.getStartTime().isAfter(Instant.now())) {
            throw new BadRequestException("A booking that has already started cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingMapper.toResponse(bookingRepository.saveAndFlush(booking));
    }

    private Booking findAccessible(UUID id, AuthenticatedUser currentUser) {
        return bookingRepository.findWithDetailsById(id)
                .filter(b -> currentUser.isAdmin() || b.getUser().getId().equals(currentUser.id()))
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }
}