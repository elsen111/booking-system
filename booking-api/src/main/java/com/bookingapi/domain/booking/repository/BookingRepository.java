package com.bookingapi.domain.booking.repository;

import com.bookingapi.domain.booking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {

    @Override
    @EntityGraph(attributePaths = {"resource", "user"})
    Page<Booking> findAll(Specification<Booking> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"resource", "user"})
    Optional<Booking> findWithDetailsById(UUID id);

    @Query("""
            select count(b) > 0 from Booking b
            where b.resource.id = :resourceId
              and b.status = com.bookingapi.domain.booking.entity.BookingStatus.CONFIRMED
              and b.startTime < :endTime
              and b.endTime > :startTime
            """)
    boolean existsOverlapping(@Param("resourceId") UUID resourceId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);
}