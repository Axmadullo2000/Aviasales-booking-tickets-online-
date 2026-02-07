package com.monolit.booking.booking.service.impl;

import com.monolit.booking.booking.dto.request.CreateBookingRequest;
import com.monolit.booking.booking.dto.request.PassengerInfoRequest;
import com.monolit.booking.booking.dto.response.BookingDetailResponse;
import com.monolit.booking.booking.dto.response.BookingResponse;
import com.monolit.booking.booking.embedded.ContactInfo;
import com.monolit.booking.booking.entity.*;
import com.monolit.booking.booking.enums.BookingStatus;
import com.monolit.booking.booking.enums.CabinClass;
import com.monolit.booking.booking.enums.PaymentStatus;
import com.monolit.booking.booking.enums.TicketStatus;
import com.monolit.booking.booking.exception.BookingExpiredException;
import com.monolit.booking.booking.exception.BookingNotFoundException;
import com.monolit.booking.booking.exception.FlightNotFoundException;
import com.monolit.booking.booking.exception.InsufficientSeatsException;
import com.monolit.booking.booking.mapper.BookingMapper;
import com.monolit.booking.booking.repo.*;
import com.monolit.booking.booking.service.interfaces.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final PassengerRepository passengerRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;
    private final TicketPdfService ticketPdfService;

    private static final String REFERENCE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int REFERENCE_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 15;
    private final SecureRandom random = new SecureRandom();

    // ═══════════════════════════════════════
    // СОЗДАНИЕ БРОНИРОВАНИЯ
    // ═══════════════════════════════════════

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, Long userId) throws InsufficientSeatsException {
        log.info("Creating booking for user: {}", userId);

        // 1. Получаем пользователя
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // 2. Валидация запроса
        validateBookingRequest(request);

        // 3. Получаем рейс и проверяем доступность
        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new FlightNotFoundException(request.getFlightId()));

        int passengerCount = request.getPassengers().size();

        // Проверяем что рейс доступен для бронирования
        if (!flight.isBookable()) {
            throw new IllegalStateException("Flight is not available for booking");
        }

        // Проверяем достаточность мест
        if (flight.getAvailableSeats() < passengerCount) {
            throw new InsufficientSeatsException("Insufficient seats in flight");
        }

        // 4. Создаём бронирование
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

        // 5. Создаём билеты для каждого пассажира
        BigDecimal totalAmount = BigDecimal.ZERO;
        CabinClass cabinClass = request.getCabinClass();
        BigDecimal ticketPrice = getFlightPrice(flight, cabinClass);

        for (PassengerInfoRequest passengerRequest : request.getPassengers()) {
            // Создаём или получаем пассажира
            Passenger passenger = createOrGetPassenger(passengerRequest, user);

            // Создаём билет
            Ticket ticket = Ticket.builder()
                    .booking(booking)
                    .flight(flight)
                    .passenger(passenger)
                    .cabinClass(cabinClass)
                    .price(ticketPrice)
                    .baseFare(ticketPrice.multiply(new BigDecimal("0.85"))) // 85% - тариф
                    .taxes(ticketPrice.multiply(new BigDecimal("0.15")))    // 15% - налоги
                    .status(TicketStatus.ISSUED)
                    .isRefundable(determineRefundability(cabinClass))
                    .isChangeable(true)
                    .checkedBaggage(getBaggageAllowance(cabinClass))
                    .handLuggage(10) // 10 кг ручной клади стандартно
                    .build();

            booking.getTickets().add(ticket);
            totalAmount = totalAmount.add(ticketPrice);
        }

        booking.setTotalAmount(totalAmount);

        // 6. Резервируем места на рейсе
        flight.reserveSeats(passengerCount);
        flightRepository.save(flight);

        // 7. Сохраняем бронирование (cascade сохранит билеты)
        booking = bookingRepository.save(booking);

        log.info("Booking created: {} with {} tickets, total: {} USD",
                booking.getBookingReference(), passengerCount, totalAmount);

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

        // TODO: отправить email с билетами
        // notificationService.sendBookingConfirmation(booking);

        return bookingMapper.toBookingResponse(booking);
    }

    // ═══════════════════════════════════════
    // ОТМЕНА БРОНИРОВАНИЯ
    // ═══════════════════════════════════════

    @Override
    @Transactional
    public BookingResponse cancelBooking(String bookingReference, Long userId, String reason) {
        log.info("Cancelling booking: {} for user: {}", bookingReference, userId);

        Booking booking = bookingRepository.findByBookingReferenceWithDetails(bookingReference)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found: " + bookingReference
                ));

        // Проверка доступа
        if (!booking.getUser().getId().equals(userId)) {
            log.warn("Access denied to booking {} for user {}", bookingReference, userId);
            throw new BookingNotFoundException("Access denied");
        }

        // Проверка возможности отмены
        if (!booking.canBeCancelled()) {
            throw new IllegalStateException(
                    "Booking cannot be cancelled in status: " + booking.getStatus()
            );
        }

        // Рассчитываем возврат
        BigDecimal refundAmount = booking.calculateRefund();

        // Отменяем бронирование
        booking.cancel(reason);
        booking.setRefundAmount(refundAmount);

        // Обновляем статус билетов
        booking.getTickets().forEach(ticket -> {
            ticket.setStatus(TicketStatus.CANCELLED);
            log.debug("Ticket {} cancelled", ticket.getId());
        });

        booking = bookingRepository.save(booking);

        log.info("Booking cancelled: {}, refund: {} USD, reason: {}",
                bookingReference, refundAmount, reason);

        // TODO: инициировать возврат денег
        // paymentService.processRefund(booking, refundAmount);

        // TODO: отправить уведомление об отмене
        // notificationService.sendBookingCancellation(booking);

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

        // ✅ Используем TicketPdfService для генерации настоящего PDF
        byte[] pdfBytes = ticketPdfService.generateTicketPdf(booking);

        log.info("PDF generated for booking: {}, size: {} bytes ({} KB)",
                bookingReference, pdfBytes.length, pdfBytes.length / 1024);

        return pdfBytes;
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

        if (request.getCabinClass() == null) {
            throw new IllegalArgumentException("Cabin class is required");
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

    private BigDecimal getFlightPrice(Flight flight, CabinClass cabinClass) {
        return switch (cabinClass) {
            case ECONOMY, PREMIUM_ECONOMY -> flight.getBasePrice();
            case BUSINESS -> flight.getBusinessPrice() != null
                    ? flight.getBusinessPrice()
                    : flight.getBasePrice().multiply(new BigDecimal("2.5"));
            case FIRST_CLASS -> flight.getFirstClassPrice() != null
                    ? flight.getFirstClassPrice()
                    : flight.getBasePrice().multiply(new BigDecimal("4.0"));
        };
    }

    private boolean determineRefundability(CabinClass cabinClass) {
        // Упрощённая логика:
        // - Эконом: не возвратный
        // - Премиум эконом: частично возвратный
        // - Бизнес и Первый класс: полностью возвратный
        return cabinClass == CabinClass.PREMIUM_ECONOMY ||
                cabinClass == CabinClass.BUSINESS ||
                cabinClass == CabinClass.FIRST_CLASS;
    }

    private Integer getBaggageAllowance(CabinClass cabinClass) {
        return switch (cabinClass) {
            case ECONOMY -> 0;              // без багажа (лоукост стиль)
            case PREMIUM_ECONOMY -> 23;     // 1 место 23 кг
            case BUSINESS -> 32;            // 1 место 32 кг
            case FIRST_CLASS -> 40;         // 2 места по 40 кг
        };
    }

    private Passenger createOrGetPassenger(PassengerInfoRequest request, Users user) {
        // Если пассажир уже сохранён у этого пользователя - используем его
        if (request.getSavedPassengerId() != null) {
            Passenger savedPassenger = passengerRepository.findById(request.getSavedPassengerId())
                    .filter(p -> p.getUser() != null && p.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Saved passenger not found: " + request.getSavedPassengerId()
                    ));

            log.debug("Using saved passenger: {} {}",
                    savedPassenger.getFirstName(), savedPassenger.getLastName());

            return savedPassenger;
        }

        // Создаём нового пассажира
        Passenger passenger = bookingMapper.toPassenger(request);

        // Если пользователь хочет сохранить пассажира для будущих бронирований
        if (Boolean.TRUE.equals(request.getSaveForFuture())) {
            passenger.setUser(user);
            passenger.setIsSaved(true);
            log.debug("Passenger will be saved for future bookings: {} {}",
                    passenger.getFirstName(), passenger.getLastName());
        }

        return passengerRepository.save(passenger);
    }

    private void expireBooking(Booking booking) {
        log.info("Expiring booking: {}", booking.getBookingReference());

        booking.setStatus(BookingStatus.EXPIRED);

        // Освобождаем места на рейсах
        booking.getTickets().forEach(ticket -> {
            Flight flight = ticket.getFlight();
            if (flight != null) {
                flight.releaseSeats(1);
                flightRepository.save(flight);
                log.debug("Released 1 seat on flight {}", flight.getFlightNumber());
            }
            ticket.setStatus(TicketStatus.VOIDED);
        });

        bookingRepository.save(booking);
        log.info("Booking expired and seats released: {}", booking.getBookingReference());
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

    // ✅ Метод generatePdfBytes() УДАЛЁН - теперь используется TicketPdfService
}
