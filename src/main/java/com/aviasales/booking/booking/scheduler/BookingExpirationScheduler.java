package com.aviasales.booking.booking.scheduler;

import com.aviasales.booking.booking.entity.Booking;
import com.aviasales.booking.booking.entity.Flight;
import com.aviasales.booking.booking.entity.Ticket;
import com.aviasales.booking.booking.enums.BookingStatus;
import com.aviasales.booking.booking.enums.CabinClass;
import com.aviasales.booking.booking.enums.TicketStatus;
import com.aviasales.booking.booking.repo.BookingRepository;
import com.aviasales.booking.booking.repo.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler для автоматической отмены истёкших бронирований
 * Запускается каждую минуту и ищет PENDING бронирования с истёкшим сроком оплаты
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpirationScheduler {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;

    /**
     * Проверка истёкших бронирований каждые 60 секунд
     */
    @Scheduled(fixedRate = 60000) // каждую минуту
    @Transactional
    public void expirePendingBookings() {
        log.debug("Running booking expiration check at {}", LocalDateTime.now());

        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(
                LocalDateTime.now()
        );

        if (expiredBookings.isEmpty()) {
            log.debug("No expired bookings found");
            return;
        }

        log.info("Found {} expired booking(s) to process", expiredBookings.size());

        int successCount = 0;
        int failureCount = 0;

        for (Booking booking : expiredBookings) {
            try {
                expireBooking(booking);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("Error expiring booking {}: {}",
                        booking.getBookingReference(), e.getMessage(), e);
            }
        }

        log.info("Booking expiration completed: {} succeeded, {} failed",
                successCount, failureCount);
    }

    /**
     * Истечь конкретное бронирование
     */
    private void expireBooking(Booking booking) {
        String bookingRef = booking.getBookingReference();
        log.info("Expiring booking: {}", bookingRef);

        // Освобождаем места на всех рейсах
        int totalSeatsReleased = 0;

        for (Ticket ticket : booking.getTickets()) {
            Flight flight = ticket.getFlight();

            if (flight != null) {
                // ✅ Освобождаем место с учетом класса кабины
                CabinClass cabinClass = ticket.getCabinClass();
                flight.releaseSeats(1, cabinClass);
                flightRepository.save(flight);
                totalSeatsReleased++;

                log.debug("Released 1 {} seat on flight {} for booking {}",
                        cabinClass, flight.getFlightNumber(), bookingRef);
            }
        }

        // Обновляем статус бронирования
        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);

        log.info("Booking {} expired. Released {} seats total",
                bookingRef, totalSeatsReleased);
    }

}
