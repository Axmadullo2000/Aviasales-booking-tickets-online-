package com.monolit.booking.booking.dataloader;

import com.monolit.booking.booking.entity.*;
import com.monolit.booking.booking.enums.*;
import com.monolit.booking.booking.repo.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class BookingDataLoader {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final PaymentRepository paymentRepository;

    @PostConstruct
    @Transactional
    public void loadData() {
        if (bookingRepository.count() > 0) {
            log.info("Booking data already loaded, skipping initialization");
            return;
        }

        log.info("Loading booking test data...");

        List<Flight> flights = flightRepository.findAll();
        if (flights.isEmpty()) {
            log.warn("No flights found, skipping booking data load");
            return;
        }

        createBookings(flights);

        log.info("Booking test data loaded successfully");
    }

    private void createBookings(List<Flight> flights) {
        // Бронирование 1: Подтвержденное (1 пассажир, эконом)
        createConfirmedBooking(
                flights.get(0), // HY501 TAS->SVO
                1L,
                1,
                SeatClass.ECONOMY,
                createPassenger("John", "Doe", "AB1234567", LocalDate.of(1990, 5, 15), "USA", "12A")
        );

        // Бронирование 2: Подтвержденное (2 пассажира, бизнес)
        createConfirmedBooking(
                flights.get(1), // HY503 TAS->SVO
                2L,
                2,
                SeatClass.BUSINESS,
                createPassenger("Alice", "Smith", "CD9876543", LocalDate.of(1985, 8, 22), "UK", "1A"),
                createPassenger("Bob", "Smith", "CD9876544", LocalDate.of(1987, 3, 10), "UK", "1B")
        );

        // Бронирование 3: В ожидании оплаты
        createPendingBooking(
                flights.get(5), // HY601 TAS->DXB
                3L,
                1,
                SeatClass.ECONOMY,
                createPassenger("Emma", "Johnson", "EF5554321", LocalDate.of(1995, 12, 5), "Canada", null)
        );

        // Бронирование 4: Подтвержденное семейное (4 пассажира, эконом)
        createConfirmedBooking(
                flights.get(7), // TK367 TAS->IST
                4L,
                4,
                SeatClass.ECONOMY,
                createPassenger("Michael", "Brown", "GH1112131", LocalDate.of(1980, 4, 18), "Germany", "15A"),
                createPassenger("Sarah", "Brown", "GH1112132", LocalDate.of(1982, 7, 25), "Germany", "15B"),
                createPassenger("Emma", "Brown", "GH1112133", LocalDate.of(2010, 9, 12), "Germany", "15C"),
                createPassenger("Oliver", "Brown", "GH1112134", LocalDate.of(2012, 11, 8), "Germany", "15D")
        );

        // Бронирование 5: Отмененное
        createCancelledBooking(
                flights.get(2), // HY505 TAS->SVO
                5L,
                1,
                SeatClass.BUSINESS,
                createPassenger("David", "Wilson", "IJ7778889", LocalDate.of(1975, 2, 28), "Australia", null)
        );

        // Бронирование 6: Подтвержденное (внутренний рейс)
        createConfirmedBooking(
                flights.get(10), // HY55 TAS->SKD
                6L,
                1,
                SeatClass.ECONOMY,
                createPassenger("Akmal", "Karimov", "KL2223334", LocalDate.of(1992, 6, 10), "Uzbekistan", "8C")
        );

        // Бронирование 7: Подтвержденное (бизнес класс)
        createConfirmedBooking(
                flights.get(6), // EK378 DXB->TAS
                7L,
                1,
                SeatClass.BUSINESS,
                createPassenger("James", "Taylor", "MN4445556", LocalDate.of(1988, 10, 3), "USA", "2A")
        );

        // Бронирование 8: В ожидании оплаты (пара, эконом)
        createPendingBooking(
                flights.get(9), // HY701 TAS->LED
                8L,
                2,
                SeatClass.ECONOMY,
                createPassenger("Anna", "Ivanova", "OP6667778", LocalDate.of(1993, 1, 20), "Russia", null),
                createPassenger("Dmitry", "Ivanov", "OP6667779", LocalDate.of(1991, 5, 15), "Russia", null)
        );

        // Бронирование 9: Подтвержденное (группа 3 человека)
        createConfirmedBooking(
                flights.get(8), // TK366 IST->TAS
                9L,
                3,
                SeatClass.ECONOMY,
                createPassenger("Fatima", "Ahmed", "QR8889990", LocalDate.of(1989, 8, 12), "Turkey", "20A"),
                createPassenger("Ayşe", "Yilmaz", "QR8889991", LocalDate.of(1990, 9, 25), "Turkey", "20B"),
                createPassenger("Mehmet", "Demir", "QR8889992", LocalDate.of(1988, 3, 7), "Turkey", "20C")
        );

        // Бронирование 10: Истекшее (не оплачено вовремя)
        createExpiredBooking(
                flights.get(4), // SU1878 SVO->TAS
                10L,
                1,
                SeatClass.ECONOMY,
                createPassenger("Ivan", "Petrov", "ST1234567", LocalDate.of(1986, 11, 30), "Russia", null)
        );

        log.info("Created 10 bookings with passengers and payments");
    }

    private void createConfirmedBooking(Flight flight, Long userId, int passengerCount,
                                        SeatClass seatClass, Passenger... passengers) {
        BigDecimal pricePerSeat = seatClass == SeatClass.ECONOMY ?
                flight.getPriceEconomy() : flight.getPriceBusiness();
        BigDecimal totalPrice = pricePerSeat.multiply(BigDecimal.valueOf(passengerCount));

        Booking booking = Booking.builder()
                .bookingReference(generateBookingReference())
                .userId(userId)
                .status(BookingStatus.CONFIRMED)
                .totalPrice(totalPrice)
                .createdAt(OffsetDateTime.now().minusDays(15))
                .confirmedAt(OffsetDateTime.now().minusDays(14))
                .expiresAt(null)
                .build();

        // Добавляем рейс
        BookingFlight bookingFlight = BookingFlight.builder()
                .flightId(flight.getId())
                .passengerCount(passengerCount)
                .seatClass(seatClass)
                .pricePerSeat(pricePerSeat)
                .totalPrice(totalPrice)
                .build();
        booking.addBookingFlight(bookingFlight);

        // Добавляем пассажиров
        for (Passenger passenger : passengers) {
            booking.addPassenger(passenger);
        }

        // СНАЧАЛА сохраняем booking, чтобы получить ID
        booking = bookingRepository.save(booking);

        // ПОТОМ создаем платеж с уже существующим booking.getId()
        createSuccessfulPayment(booking);

        // Обновляем доступные места
        flight.setAvailableSeats(flight.getAvailableSeats() - passengerCount);
        flightRepository.save(flight);
    }

    private void createPendingBooking(Flight flight, Long userId, int passengerCount,
                                      SeatClass seatClass, Passenger... passengers) {
        BigDecimal pricePerSeat = seatClass == SeatClass.ECONOMY ?
                flight.getPriceEconomy() : flight.getPriceBusiness();
        BigDecimal totalPrice = pricePerSeat.multiply(BigDecimal.valueOf(passengerCount));

        Booking booking = Booking.builder()
                .bookingReference(generateBookingReference())
                .userId(userId)
                .status(BookingStatus.PENDING)
                .totalPrice(totalPrice)
                .createdAt(OffsetDateTime.now().minusHours(2))
                .expiresAt(OffsetDateTime.now().plusHours(22))
                .build();

        BookingFlight bookingFlight = BookingFlight.builder()
                .flightId(flight.getId())
                .passengerCount(passengerCount)
                .seatClass(seatClass)
                .pricePerSeat(pricePerSeat)
                .totalPrice(totalPrice)
                .build();
        booking.addBookingFlight(bookingFlight);

        for (Passenger passenger : passengers) {
            booking.addPassenger(passenger);
        }

        // СНАЧАЛА сохраняем
        booking = bookingRepository.save(booking);

        // ПОТОМ создаем платеж
        createPendingPayment(booking);
    }

    private void createCancelledBooking(Flight flight, Long userId, int passengerCount,
                                        SeatClass seatClass, Passenger... passengers) {
        BigDecimal pricePerSeat = seatClass == SeatClass.ECONOMY ?
                flight.getPriceEconomy() : flight.getPriceBusiness();
        BigDecimal totalPrice = pricePerSeat.multiply(BigDecimal.valueOf(passengerCount));

        Booking booking = Booking.builder()
                .bookingReference(generateBookingReference())
                .userId(userId)
                .status(BookingStatus.CANCELLED)
                .totalPrice(totalPrice)
                .createdAt(OffsetDateTime.now().minusDays(20))
                .confirmedAt(OffsetDateTime.now().minusDays(19))
                .build();

        BookingFlight bookingFlight = BookingFlight.builder()
                .flightId(flight.getId())
                .passengerCount(passengerCount)
                .seatClass(seatClass)
                .pricePerSeat(pricePerSeat)
                .totalPrice(totalPrice)
                .build();
        booking.addBookingFlight(bookingFlight);

        for (Passenger passenger : passengers) {
            booking.addPassenger(passenger);
        }

        // СНАЧАЛА сохраняем
        booking = bookingRepository.save(booking);

        // ПОТОМ создаем платеж
        createRefundedPayment(booking);
    }

    private void createExpiredBooking(Flight flight, Long userId, int passengerCount,
                                      SeatClass seatClass, Passenger... passengers) {
        BigDecimal pricePerSeat = seatClass == SeatClass.ECONOMY ?
                flight.getPriceEconomy() : flight.getPriceBusiness();
        BigDecimal totalPrice = pricePerSeat.multiply(BigDecimal.valueOf(passengerCount));

        Booking booking = Booking.builder()
                .bookingReference(generateBookingReference())
                .userId(userId)
                .status(BookingStatus.EXPIRED)
                .totalPrice(totalPrice)
                .createdAt(OffsetDateTime.now().minusDays(3))
                .expiresAt(OffsetDateTime.now().minusDays(2))
                .build();

        BookingFlight bookingFlight = BookingFlight.builder()
                .flightId(flight.getId())
                .passengerCount(passengerCount)
                .seatClass(seatClass)
                .pricePerSeat(pricePerSeat)
                .totalPrice(totalPrice)
                .build();
        booking.addBookingFlight(bookingFlight);

        for (Passenger passenger : passengers) {
            booking.addPassenger(passenger);
        }

        // СНАЧАЛА сохраняем
        booking = bookingRepository.save(booking);

        // ПОТОМ создаем платеж
        createFailedPayment(booking);
    }

    private Passenger createPassenger(String firstName, String lastName, String passportNumber,
                                      LocalDate dateOfBirth, String nationality, String seatNumber) {
        return Passenger.builder()
                .firstName(firstName)
                .lastName(lastName)
                .passportNumber(passportNumber)
                .dateOfBirth(dateOfBirth)
                .nationality(nationality)
                .seatNumber(seatNumber)
                .build();
    }

    private void createSuccessfulPayment(Booking booking) {
        Payment payment = Payment.builder()
                .bookingId(booking.getId()) // Теперь ID уже существует
                .bookingReference(booking.getBookingReference())
                .amount(booking.getTotalPrice())
                .currency("USD")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.COMPLETED)
                .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .cardLastFour("4242")
                .createdAt(booking.getCreatedAt())
                .processedAt(booking.getConfirmedAt())
                .build();

        paymentRepository.save(payment);
    }

    private void createPendingPayment(Booking booking) {
        Payment payment = Payment.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .amount(booking.getTotalPrice())
                .currency("USD")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .createdAt(booking.getCreatedAt())
                .build();

        paymentRepository.save(payment);
    }

    private void createRefundedPayment(Booking booking) {
        Payment payment = Payment.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .amount(booking.getTotalPrice())
                .currency("USD")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.REFUNDED)
                .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .cardLastFour("5555")
                .createdAt(booking.getCreatedAt())
                .processedAt(booking.getCreatedAt().plusHours(1))
                .build();

        paymentRepository.save(payment);
    }

    private void createFailedPayment(Booking booking) {
        Payment payment = Payment.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .amount(booking.getTotalPrice())
                .currency("USD")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.FAILED)
                .cardLastFour("0000")
                .createdAt(booking.getCreatedAt())
                .failureReason("Insufficient funds")
                .build();

        paymentRepository.save(payment);
    }

    private String generateBookingReference() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
