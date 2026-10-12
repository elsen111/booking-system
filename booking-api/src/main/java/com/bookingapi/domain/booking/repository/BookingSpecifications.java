package com.bookingapi.domain.booking.repository;

import com.bookingapi.domain.booking.entity.Booking;
import com.bookingapi.domain.booking.dto.BookingFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class BookingSpecifications {

    private BookingSpecifications() {
    }

    public static Specification<Booking> from(BookingFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.userId() != null) {
                predicates.add(cb.equal(root.get("user").get("id"), filter.userId()));
            }
            if (filter.resourceId() != null) {
                predicates.add(cb.equal(root.get("resource").get("id"), filter.resourceId()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            // Overlap with [from, to): booking ends after `from` and starts before `to`
            if (filter.from() != null) {
                predicates.add(cb.greaterThan(root.<Instant>get("endTime"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThan(root.<Instant>get("startTime"), filter.to()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}