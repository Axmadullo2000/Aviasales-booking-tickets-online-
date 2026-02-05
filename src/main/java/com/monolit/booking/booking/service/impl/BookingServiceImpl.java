package com.monolit.booking.booking.service.impl;

import com.monolit.booking.booking.dto.request.*;
import com.monolit.booking.booking.dto.response.*;
import com.monolit.booking.booking.entity.*;
import com.monolit.booking.booking.enums.*;
import com.monolit.booking.booking.exception.*;
import com.monolit.booking.booking.mapper.BookingMapper;
import com.monolit.booking.booking.mapper.FlightMapper;
import com.monolit.booking.booking.repo.*;
import com.monolit.booking.booking.service.interfaces.BookingService;
import com.monolit.booking.booking.service.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final BookingMapper bookingMapper;
    private final FlightMapper flightMapper;
    private final BookingValidationService validationService;
    private final TicketPdfService ticketPdfService;
    private final NotificationService notificationService;

    private static final String REFERENCE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int REFERENCE_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 15;
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, Long userId) {
        log.info("Creating booking for user: {}", userId);

        validationService.validateBookingRequest(request);

        String bookingReference = generateUniqueBookingReference();
        int passengerCount = request.getPassengers().size();

        BigDecimal totalPrice = BigDecimal.ZERO;
        Booking booking = Booking.builder()
                .bookingReference(bookingReference)
                .userId(userId)
                .status(BookingStatus.PENDING)
                .totalPrice(BigDecimal.ZERO)
                .expiresAt(OffsetDateTime.now().plusMinutes(EXPIRATION_MINUTES))
                .build();

        for (BookingFlightRequest flightRequest : request.getFlights()) {
            Flight flight = validationService.getAndValidateFlight(flightRequest.getFlightId(), passengerCount);

            flight.reserveSeats(passengerCount);
            flightRepository.save(flight);

            BigDecimal pricePerSeat = flightRequest.getSeatClass() == SeatClass.BUSINESS
                    ? flight.getPriceBusiness()
                    : flight.getPriceEconomy();
            BigDecimal flightTotal = pricePerSeat.multiply(BigDecimal.valueOf(passengerCount));

            BookingFlight bookingFlight = BookingFlight.builder()
                    .flightId(flight.getId())
                    .passengerCount(passengerCount)
                    .seatClass(flightRequest.getSeatClass())
                    .pricePerSeat(pricePerSeat)
                    .totalPrice(flightTotal)
                    .build();

            booking.addBookingFlight(bookingFlight);
            totalPrice = totalPrice.add(flightTotal);
        }

        List<Passenger> passengers = bookingMapper.toPassengerList(request.getPassengers());
        for (Passenger passenger : passengers) {
            booking.addPassenger(passenger);
        }

        booking.setTotalPrice(totalPrice);
        booking = bookingRepository.save(booking);

        log.info("Booking created with reference: {}", bookingReference);
        notificationService.notifyBookingCreated(booking);

        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingByReference(String bookingReference, Long userId) {
        log.info("Getting booking by reference: {} for user: {}", bookingReference, userId);

        Booking booking = bookingRepository.findByBookingReferenceWithDetails(bookingReference)
                .orElseThrow(() -> new BookingNotFoundException(bookingReference, true));

        if (!booking.getUserId().equals(userId)) {
            throw new BookingNotFoundException("Booking not found or access denied");
        }

        BookingDetailResponse response = bookingMapper.toBookingDetailResponse(booking);

        List<BookingFlightResponse> flightResponses = booking.getBookingFlights().stream()
                .map(bf -> {
                    BookingFlightResponse bfr = bookingMapper.toBookingFlightResponse(bf);
                    Flight flight = flightRepository.findById(bf.getFlightId())
                            .orElseThrow(() -> new FlightNotFoundException(bf.getFlightId()));
                    bfr.setFlight(flightMapper.toFlightDetailResponse(flight));
                    return bfr;
                })
                .collect(Collectors.toList());

        response.setFlights(flightResponses);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getUserBookings(Long userId, Pageable pageable) {
        log.info("Getting bookings for user: {}", userId);
        return bookingRepository.findByUserId(userId, pageable)
                .map(bookingMapper::toBookingResponse);
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(String bookingReference, Long userId) {
        log.info("Confirming booking: {} for user: {}", bookingReference, userId);

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new BookingNotFoundException(bookingReference, true));

        if (!booking.getUserId().equals(userId)) {
            throw new BookingNotFoundException("Booking not found or access denied");
        }

        if (booking.isExpired()) {
            expireBooking(booking);
            throw new BookingExpiredException(bookingReference);
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking is not in PENDING status");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(OffsetDateTime.now());
        booking.setExpiresAt(null);

        booking = bookingRepository.save(booking);
        log.info("Booking confirmed: {}", bookingReference);

        notificationService.notifyBookingConfirmed(booking);

        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(String bookingReference, Long userId) {
        log.info("Cancelling booking: {} for user: {}", bookingReference, userId);

        Booking booking = bookingRepository.findByBookingReferenceWithDetails(bookingReference)
                .orElseThrow(() -> new BookingNotFoundException(bookingReference, true));

        if (!booking.getUserId().equals(userId)) {
            throw new BookingNotFoundException("Booking not found or access denied");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed booking");
        }

        for (BookingFlight bookingFlight : booking.getBookingFlights()) {
            Flight flight = flightRepository.findById(bookingFlight.getFlightId()).orElse(null);
            if (flight != null) {
                flight.releaseSeats(bookingFlight.getPassengerCount());
                flightRepository.save(flight);
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);

        log.info("Booking cancelled: {}", bookingReference);
        notificationService.notifyBookingCancelled(booking);

        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateTicketPdf(String bookingReference, Long userId) {
        log.info("Generating ticket PDF for booking: {} user: {}", bookingReference, userId);

        Booking booking = bookingRepository.findByBookingReferenceWithDetails(bookingReference)
                .orElseThrow(() -> new BookingNotFoundException(bookingReference, true));

        if (!booking.getUserId().equals(userId)) {
            throw new BookingNotFoundException("Booking not found or access denied");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot generate ticket for unconfirmed booking");
        }

        return ticketPdfService.generateTicketPdf(booking);
    }

    private void expireBooking(Booking booking) {
        log.info("Expiring booking: {}", booking.getBookingReference());

        for (BookingFlight bookingFlight : booking.getBookingFlights()) {
            Flight flight = flightRepository.findById(bookingFlight.getFlightId()).orElse(null);
            if (flight != null) {
                flight.releaseSeats(bookingFlight.getPassengerCount());
                flightRepository.save(flight);
            }
        }

        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);
    }

    private String generateUniqueBookingReference() {
        String reference;
        do {
            reference = generateBookingReference();
        } while (bookingRepository.existsByBookingReference(reference));
        return reference;
    }

    private String generateBookingReference() {
        StringBuilder sb = new StringBuilder(REFERENCE_LENGTH);
        for (int i = 0; i < REFERENCE_LENGTH; i++) {
            sb.append(REFERENCE_CHARS.charAt(random.nextInt(REFERENCE_CHARS.length())));
        }
        return sb.toString();
    }
}
