package com.monolit.booking.booking.repo;

import com.monolit.booking.booking.entity.Booking;
import com.monolit.booking.booking.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.expiresAt < :now")
    List<Booking> findExpiredBookings(@Param("status") BookingStatus status, @Param("now") OffsetDateTime now);

    boolean existsByBookingReference(String bookingReference);
    @EntityGraph(attributePaths = {"bookingFlights", "passengers"})
    @Query("SELECT DISTINCT b FROM Booking b WHERE b.bookingReference = :reference")
    Optional<Booking> findByBookingReferenceWithDetails(@Param("reference") String reference);
}
