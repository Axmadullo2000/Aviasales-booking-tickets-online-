package com.monolit.booking.booking.repo;

import com.monolit.booking.booking.entity.BookingFlight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingFlightRepository extends JpaRepository<BookingFlight, Long> {

    List<BookingFlight> findByBookingId(Long bookingId);

    List<BookingFlight> findByFlightId(Long flightId);
}
