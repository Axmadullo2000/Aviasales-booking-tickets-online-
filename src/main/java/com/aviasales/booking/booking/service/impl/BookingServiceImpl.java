package com.aviasales.booking.booking.service.impl;

import com.aviasales.booking.booking.dto.request.CreateBookingRequest;
import com.aviasales.booking.booking.dto.request.PassengerInfoRequest;
import com.aviasales.booking.booking.dto.response.AvailableSeatsResponse;
import com.aviasales.booking.booking.dto.response.BookingDetailResponse;
import com.aviasales.booking.booking.dto.response.BookingResponse;
import com.aviasales.booking.booking.embedded.ContactInfo;
import com.aviasales.booking.booking.entity.*;
import com.aviasales.booking.booking.repo.*;
import com.aviasales.booking.booking.service.interfaces.PricingService;
import com.aviasales.booking.booking.service.interfaces.SeatSelectionService;
import com.aviasales.booking.booking.service.interfaces.TicketPdfService;
import com.aviasales.booking.booking.enums.BookingStatus;
import com.aviasales.booking.booking.enums.CabinClass;
import com.aviasales.booking.booking.enums.PaymentStatus;
import com.aviasales.booking.booking.enums.TicketStatus;
import com.aviasales.booking.booking.exception.BookingExpiredException;
import com.aviasales.booking.booking.exception.BookingNotFoundException;
import com.aviasales.booking.booking.exception.FlightNotFoundException;
import com.aviasales.booking.booking.exception.InsufficientSeatsException;
import com.aviasales.booking.booking.mapper.BookingMapper;
import com.aviasales.booking.booking.service.interfaces.BookingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final PassengerRepository passengerRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;
    private final TicketPdfService ticketPdfService;
    private final SeatSelectionService seatSelectionService;
    private final PricingService pricingService;

    private static final String REFERENCE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int REFERENCE_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 15;
    private final SecureRandom random = new SecureRandom();

    // ═══════════════════════════════════════
    // СОЗДАНИЕ БРОНИРОВАНИЯ (С ПОДДЕРЖКОЙ РАЗНЫХ КЛАССОВ)
    // ═══════════════════════════════════════

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, Long userId)
            throws InsufficientSeatsException {
        log.info("Creating booking for user: {}", userId);

        // 1. Получаем пользователя
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // 2. Валидация запроса
        validateBookingRequest(request);

        // 3. Получаем рейс
        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new FlightNotFoundException(request.getFlightId()));

        // Проверяем что рейс доступен для бронирования
        if (!flight.isBookable()) {
            throw new IllegalStateException("Flight is not available for booking");
        }

        // 4. ✅ НОВАЯ ЛОГИКА: Подсчет мест по классам
        CabinClass defaultClass = request.getDefaultCabinClass() != null
                ? request.getDefaultCabinClass()
                : CabinClass.ECONOMY;

        Map<CabinClass, Integer> seatsByClass = new HashMap<>();

        for (PassengerInfoRequest passengerRequest : request.getPassengers()) {
            CabinClass passengerClass = passengerRequest.getCabinClass() != null
                    ? passengerRequest.getCabinClass()
                    : defaultClass;

            seatsByClass.merge(passengerClass, 1, Integer::sum);
        }

        // 5. Проверяем доступность мест для каждого класса
        for (Map.Entry<CabinClass, Integer> entry : seatsByClass.entrySet()) {
            CabinClass cabinClass = entry.getKey();
            int requiredSeats = entry.getValue();

            if (!pricingService.hasEnoughSeats(flight, cabinClass, requiredSeats)) {
                throw new InsufficientSeatsException(
                        String.format("Insufficient %s seats. Requested: %d, Available: %d",
                                cabinClass,
                                requiredSeats,
                                pricingService.getAvailableSeatsForClass(flight, cabinClass))
                );
            }
        }

        // 6. Создаём бронирование
        Booking booking = Booking.builder()
                .bookingReference(generateUniqueBookingReference())
                .user(user)
                .status(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .expiresAt(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES))
                .contactInfo(createContactInfo(request))
                .specialRequests(request.getSpecialRequests())
                .tickets(new ArrayList<>())
                .build();

        // 7. ✅ СОЗДАЕМ БИЛЕТЫ С УЧЕТОМ ИНДИВИДУАЛЬНОГО КЛАССА
        BigDecimal totalAmount = BigDecimal.ZERO;
        LocalDate bookingDate = LocalDate.now();

        for (PassengerInfoRequest passengerRequest : request.getPassengers()) {
            Passenger passenger = createOrGetPassenger(passengerRequest, user);

            // Определяем класс для этого пассажира
            CabinClass passengerClass = passengerRequest.getCabinClass() != null
                    ? passengerRequest.getCabinClass()
                    : defaultClass;

            // Получаем цену для его класса (с динамическим ценообразованием)
            BigDecimal ticketPrice = pricingService.calculateDynamicPrice(
                    flight,
                    passengerClass,
                    bookingDate
            ).getTotalPrice();

            // Назначаем место
            String assignedSeat = seatSelectionService.assignSeat(
                    flight,
                    passengerClass,
                    passengerRequest.getSeatNumber(),
                    passengerRequest.getSeatPreference()
            );

            log.info("Assigned {} class seat {} to passenger {} {} (price: {})",
                    passengerClass, assignedSeat,
                    passenger.getFirstName(), passenger.getLastName(), ticketPrice);

            // Создаём билет
            Ticket ticket = Ticket.builder()
                    .booking(booking)
                    .flight(flight)
                    .passenger(passenger)
                    .cabinClass(passengerClass)  // ✅ Индивидуальный класс
                    .seatNumber(assignedSeat)
                    .seatPreference(passengerRequest.getSeatPreference())
                    .price(ticketPrice)
                    .baseFare(ticketPrice.multiply(new BigDecimal("0.85")))
                    .taxes(ticketPrice.multiply(new BigDecimal("0.15")))
                    .status(TicketStatus.ISSUED)
                    .isRefundable(determineRefundability(passengerClass))
                    .isChangeable(true)
                    .checkedBaggage(getBaggageAllowance(passengerClass))
                    .handLuggage(10)
                    .build();

            booking.getTickets().add(ticket);
            totalAmount = totalAmount.add(ticketPrice);
        }

        booking.setTotalAmount(totalAmount);

        // 8. ✅ Резервируем места по классам
        for (Map.Entry<CabinClass, Integer> entry : seatsByClass.entrySet()) {
            flight.reserveSeats(entry.getValue(), entry.getKey());
            log.debug("Reserved {} {} seats on flight {}",
                    entry.getValue(), entry.getKey(), flight.getFlightNumber());
        }
        flightRepository.save(flight);

        // 9. Сохраняем бронирование (cascade сохранит билеты)
        booking = bookingRepository.save(booking);

        log.info("Booking created: {} with {} tickets (mixed classes: {}), total: {} USD",
                booking.getBookingReference(),
                request.getPassengers().size(),
                seatsByClass,
                totalAmount);

        return bookingMapper.toBookingResponse(booking);
    }

    // ═══════════════════════════════════════
    // ПОЛУЧЕНИЕ БРОНИРОВАНИЯ
    // ═══════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingByReference(String bookingReference, Long userId) {
        log.info("Getting booking: {} for user: {}", bookingReference, userId);

        Booking booking = bookingRepository.findByBookingReferenceWithDetails(bookingReference)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found: " + bookingReference
                ));

        // Проверка доступа
        if (!booking.getUser().getId().equals(userId)) {
            log.warn("Access denied to booking {} for user {}", bookingReference, userId);
            throw new BookingNotFoundException("Access denied");
        }

        return bookingMapper.toBookingDetailResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getUserBookings(Long userId, Pageable pageable) {
        log.info("Getting bookings for user: {}", userId);

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        return bookingRepository.findByUser(user, pageable)
                .map(bookingMapper::toBookingResponse);
    }

    // ═══════════════════════════════════════
    // ПОДТВЕРЖДЕНИЕ БРОНИРОВАНИЯ (ОПЛАТА)
    // ═══════════════════════════════════════

    @Override
    @Transactional
    public BookingResponse confirmBooking(String bookingReference, Long userId) {
        log.info("Confirming booking: {} for user: {}", bookingReference, userId);

        Booking booking = bookingRepository.findByBookingReferenceWithDetails(bookingReference)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found: " + bookingReference
                ));

        // Проверка доступа
        if (!booking.getUser().getId().equals(userId)) {
            log.warn("Access denied to booking {} for user {}", bookingReference, userId);
            throw new BookingNotFoundException("Access denied");
        }

        // Проверка срока действия
        if (booking.isExpired()) {
            log.warn("Booking {} has expired", bookingReference);
            expireBooking(booking);
            throw new BookingExpiredException(bookingReference);
        }

        // Проверка текущего статуса
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot confirm booking in status: " + booking.getStatus()
            );
        }

        // Подтверждаем бронирование
        booking.confirm();

        // Обновляем статус билетов
        booking.getTickets().forEach(ticket -> {
            ticket.setStatus(TicketStatus.CONFIRMED);
            log.debug("Ticket {} confirmed for passenger {}",
                    ticket.getId(), ticket.getPassenger().getFirstName());
        });

        booking = bookingRepository.save(booking);

        log.info("Booking confirmed: {}, amount: {} USD",
                bookingReference, booking.getTotalAmount());

        return bookingMapper.toBookingResponse(booking);
    }

    // ═══════════════════════════════════════
    // ОТМЕНА БРОНИРОВАНИЯ
    // ═══════════════════════════════════════

    @Override
    @Transactional
    public BookingResponse cancelBooking(String reference, Long userId, String reason)
            throws AccessDeniedException {
        log.info("Cancelling booking: {} by user: {}, reason: {}", reference, userId, reason);

        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found with reference: " + reference));

        if (!booking.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Not authorized to cancel this booking");
        }

        if (!isCancellable(booking)) {
            throw new IllegalStateException(
                    "Booking cannot be cancelled. Status: " + booking.getStatus());
        }

        // Освобождаем места и отменяем билеты
        Map<Flight, Map<CabinClass, Long>> seatsToRelease = new HashMap<>();
        BigDecimal totalRefund = BigDecimal.ZERO;

        for (Ticket ticket : booking.getTickets()) {
            Flight flight = ticket.getFlight();
            if (flight != null) {
                long hoursUntilDeparture = flight.getHoursUntilDeparture();

                // Используем метод cancel из Ticket
                BigDecimal refund = ticket.cancel(hoursUntilDeparture);
                totalRefund = totalRefund.add(refund);

                log.debug("Ticket {} cancelled. Refund: {} ({}% of {})",
                        ticket.getTicketNumber(),
                        refund,
                        ticket.getRefundPercentage(hoursUntilDeparture),
                        ticket.getPrice());

                // Группируем для освобождения мест
                seatsToRelease
                        .computeIfAbsent(flight, k -> new HashMap<>())
                        .merge(ticket.getCabinClass(), 1L, Long::sum);
            }
        }

        // Освобождаем места
        int totalSeatsReleased = 0;
        for (Map.Entry<Flight, Map<CabinClass, Long>> flightEntry : seatsToRelease.entrySet()) {
            Flight flight = flightEntry.getKey();

            for (Map.Entry<CabinClass, Long> classEntry : flightEntry.getValue().entrySet()) {
                CabinClass cabinClass = classEntry.getKey();
                int count = classEntry.getValue().intValue();

                flight.releaseSeats(count, cabinClass);
                totalSeatsReleased += count;

                log.debug("Released {} {} seats on flight {}",
                        count, cabinClass, flight.getFlightNumber());
            }

            flightRepository.save(flight);
        }

        // Обновляем статус бронирования
        booking.markAsCancelled(reason);
        booking.setRefundAmount(totalRefund);

        booking = bookingRepository.save(booking);

        log.info("Booking {} cancelled. Released {} seats. Total refund: {}",
                reference, totalSeatsReleased, totalRefund);

        return bookingMapper.toBookingResponse(booking);
    }

    // ═══════════════════════════════════════
    // ГЕНЕРАЦИЯ PDF БИЛЕТА
    // ═══════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public byte[] generateTicketPdf(String bookingReference, Long userId) {
        log.info("Generating ticket PDF for booking: {}", bookingReference);

        Booking booking = bookingRepository.findByBookingReferenceWithDetails(bookingReference)
                .orElseThrow(() -> new BookingNotFoundException(bookingReference));

        // Проверка доступа
        if (!booking.getUser().getId().equals(userId)) {
            log.warn("Access denied to booking {} for user {}", bookingReference, userId);
            throw new BookingNotFoundException("Access denied");
        }

        // Проверка статуса
        if (booking.getStatus() != BookingStatus.CONFIRMED &&
                booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Cannot generate ticket for booking in status: " + booking.getStatus()
            );
        }

        byte[] pdfBytes = ticketPdfService.generateTicketPdf(booking);

        log.info("PDF generated for booking: {}, size: {} bytes ({} KB)",
                bookingReference, pdfBytes.length, pdfBytes.length / 1024);

        return pdfBytes;
    }

    // ═══════════════════════════════════════
    // ДОСТУПНЫЕ МЕСТА
    // ═══════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public AvailableSeatsResponse getAvailableSeats(Long flightId, CabinClass cabinClass) {
        log.info("Getting available seats for flight: {}, class: {}", flightId, cabinClass);

        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new FlightNotFoundException(flightId));

        Set<String> occupiedSeats = seatSelectionService.getOccupiedSeats(flightId);

        Integer totalSeats = getTotalSeatsForClass(flight, cabinClass);

        Set<String> occupiedInClass = occupiedSeats.stream()
                .filter(seat -> seatBelongsToClass(seat, cabinClass))
                .collect(Collectors.toSet());

        return AvailableSeatsResponse.builder()
                .flightId(flightId)
                .flightNumber(flight.getFlightNumber())
                .cabinClass(cabinClass)
                .occupiedSeats(occupiedInClass)
                .totalSeats(totalSeats)
                .availableSeats(totalSeats - occupiedInClass.size())
                .build();
    }

    // ═══════════════════════════════════════
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ - ВАЛИДАЦИЯ
    // ═══════════════════════════════════════

    private void validateBookingRequest(CreateBookingRequest request) {
        if (request.getFlightId() == null) {
            throw new IllegalArgumentException("Flight ID is required");
        }

        if (request.getPassengers() == null || request.getPassengers().isEmpty()) {
            throw new IllegalArgumentException("At least one passenger is required");
        }

        if (request.getPassengers().size() > 9) {
            throw new IllegalArgumentException("Maximum 9 passengers per booking");
        }

        if (request.getContactEmail() == null || request.getContactEmail().isBlank()) {
            throw new IllegalArgumentException("Contact email is required");
        }

        if (request.getContactPhone() == null || request.getContactPhone().isBlank()) {
            throw new IllegalArgumentException("Contact phone is required");
        }

        // Валидация каждого пассажира
        for (PassengerInfoRequest passenger : request.getPassengers()) {
            validatePassenger(passenger);
        }
    }

    private void validatePassenger(PassengerInfoRequest passenger) {
        if (passenger.getFirstName() == null || passenger.getFirstName().isBlank()) {
            throw new IllegalArgumentException("Passenger first name is required");
        }

        if (passenger.getLastName() == null || passenger.getLastName().isBlank()) {
            throw new IllegalArgumentException("Passenger last name is required");
        }

        if (passenger.getPassportNumber() == null || passenger.getPassportNumber().isBlank()) {
            throw new IllegalArgumentException("Passport number is required");
        }

        if (passenger.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Date of birth is required");
        }

        if (passenger.getNationality() == null || passenger.getNationality().isBlank()) {
            throw new IllegalArgumentException("Nationality is required");
        }

        if (passenger.getGender() == null) {
            throw new IllegalArgumentException("Gender is required");
        }

        if (passenger.getPassportCountry() == null || passenger.getPassportCountry().isBlank()) {
            throw new IllegalArgumentException("Passport country is required");
        }

        if (passenger.getPassportExpiry() == null) {
            throw new IllegalArgumentException("Passport expiry date is required");
        }
    }

    // ═══════════════════════════════════════
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ - БИЗНЕС-ЛОГИКА
    // ═══════════════════════════════════════

    private ContactInfo createContactInfo(CreateBookingRequest request) {
        return ContactInfo.builder()
                .email(request.getContactEmail())
                .phone(request.getContactPhone())
                .build();
    }

    private boolean determineRefundability(CabinClass cabinClass) {
        return cabinClass == CabinClass.PREMIUM_ECONOMY ||
                cabinClass == CabinClass.BUSINESS ||
                cabinClass == CabinClass.FIRST_CLASS;
    }

    private Integer getBaggageAllowance(CabinClass cabinClass) {
        return switch (cabinClass) {
            case ECONOMY -> 0;
            case PREMIUM_ECONOMY -> 23;
            case BUSINESS -> 32;
            case FIRST_CLASS -> 40;
        };
    }

    private Passenger createOrGetPassenger(PassengerInfoRequest request, Users user) {
        Passenger passenger = bookingMapper.toPassenger(request);

        if (Boolean.TRUE.equals(request.getSaveForFuture())) {
            passenger.setUser(user);
            passenger.setIsSaved(true);
            log.debug("Passenger will be saved for future bookings: {} {}",
                    passenger.getFirstName(), passenger.getLastName());
        }

        return passengerRepository.save(passenger);
    }

    private void expireBooking(Booking booking) {
        String bookingRef = booking.getBookingReference();
        log.info("Expiring booking: {}", bookingRef);

        Map<Flight, Map<CabinClass, Long>> seatsToRelease = new HashMap<>();

        for (Ticket ticket : booking.getTickets()) {
            Flight flight = ticket.getFlight();
            if (flight != null) {
                seatsToRelease
                        .computeIfAbsent(flight, k -> new HashMap<>())
                        .merge(ticket.getCabinClass(), 1L, Long::sum);
            }
            ticket.setStatus(TicketStatus.VOIDED);
        }

        int totalSeatsReleased = 0;
        for (Map.Entry<Flight, Map<CabinClass, Long>> flightEntry : seatsToRelease.entrySet()) {
            Flight flight = flightEntry.getKey();

            for (Map.Entry<CabinClass, Long> classEntry : flightEntry.getValue().entrySet()) {
                CabinClass cabinClass = classEntry.getKey();
                int count = classEntry.getValue().intValue();

                flight.releaseSeats(count, cabinClass);
                totalSeatsReleased += count;

                log.debug("Released {} {} seats on flight {} for expired booking {}",
                        count, cabinClass, flight.getFlightNumber(), bookingRef);
            }

            flightRepository.save(flight);
        }

        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);

        log.info("Booking {} expired. Released {} seats total", bookingRef, totalSeatsReleased);
    }

    private boolean isCancellable(Booking booking) {
        return booking.getStatus() == BookingStatus.CONFIRMED
                || booking.getStatus() == BookingStatus.PENDING;
    }

    private boolean seatBelongsToClass(String seat, CabinClass cabinClass) {
        int row = Integer.parseInt(seat.replaceAll("[^0-9]", ""));
        return switch (cabinClass) {
            case FIRST_CLASS -> row >= 1 && row <= 2;
            case BUSINESS -> row >= 3 && row <= 8;
            case ECONOMY, PREMIUM_ECONOMY -> row >= 9 && row <= 35;
        };
    }

    private Integer getTotalSeatsForClass(Flight flight, CabinClass cabinClass) {
        return switch (cabinClass) {
            case FIRST_CLASS -> flight.getFirstClassSeats() != null ? flight.getFirstClassSeats() : 0;
            case BUSINESS -> flight.getBusinessSeats() != null ? flight.getBusinessSeats() : 0;
            case ECONOMY, PREMIUM_ECONOMY -> flight.getEconomySeats() != null ? flight.getEconomySeats() : 0;
        };
    }

    // ═══════════════════════════════════════
    // ГЕНЕРАЦИЯ УНИКАЛЬНОГО РЕФЕРЕНСА
    // ═══════════════════════════════════════

    private String generateUniqueBookingReference() {
        String reference;
        int attempts = 0;

        do {
            reference = generateBookingReference();
            attempts++;

            if (attempts > 100) {
                log.error("Failed to generate unique booking reference after 100 attempts");
                throw new IllegalStateException(
                        "Failed to generate unique booking reference after 100 attempts"
                );
            }
        } while (bookingRepository.existsByBookingReference(reference));

        log.debug("Generated unique booking reference: {} (attempts: {})", reference, attempts);
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
