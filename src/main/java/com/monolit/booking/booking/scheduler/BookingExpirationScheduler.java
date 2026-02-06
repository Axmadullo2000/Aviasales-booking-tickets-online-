package com.monolit.booking.booking.scheduler;

import com.monolit.booking.booking.entity.Booking;
import com.monolit.booking.booking.entity.BookingFlight;
import com.monolit.booking.booking.entity.Flight;
import com.monolit.booking.booking.enums.BookingStatus;
import com.monolit.booking.booking.repo.BookingRepository;
import com.monolit.booking.booking.repo.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpirationScheduler {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expirePendingBookings() {
        log.debug("Running booking expiration check...");

        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(
                BookingStatus.PENDING,
                Instant.now()
        );

        if (expiredBookings.isEmpty()) {
            log.debug("No expired bookings found");
            return;
        }

        log.info("Found {} expired bookings to process", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            try {
                expireBooking(booking);
            } catch (Exception e) {
                log.error("Error expiring booking {}: {}", booking.getBookingReference(), e.getMessage());
            }
        }
    }

    private void expireBooking(Booking booking) {
        log.info("Expiring booking: {}", booking.getBookingReference());

        for (BookingFlight bookingFlight : booking.getBookingFlights()) {
            Flight flight = flightRepository.findById(bookingFlight.getFlightId()).orElse(null);
            if (flight != null) {
                flight.releaseSeats(bookingFlight.getPassengerCount());
                flightRepository.save(flight);
                log.debug("Released {} seats for flight {}",
                        bookingFlight.getPassengerCount(), flight.getFlightNumber());
            }
        }

        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);

        log.info("Booking {} expired successfully", booking.getBookingReference());
    }
}
