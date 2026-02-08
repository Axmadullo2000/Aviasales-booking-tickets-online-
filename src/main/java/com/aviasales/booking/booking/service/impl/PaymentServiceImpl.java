package com.aviasales.booking.booking.service.impl;

import com.aviasales.booking.booking.dto.request.ConfirmPaymentRequest;
import com.aviasales.booking.booking.dto.request.CreatePaymentRequest;
import com.aviasales.booking.booking.dto.response.PaymentResponse;
import com.aviasales.booking.booking.dto.response.PaymentStatusResponse;
import com.aviasales.booking.booking.entity.Booking;
import com.aviasales.booking.booking.entity.Payment;
import com.aviasales.booking.booking.enums.BookingStatus;
import com.aviasales.booking.booking.enums.CardType;
import com.aviasales.booking.booking.enums.PaymentStatus;
import com.aviasales.booking.booking.exception.BookingExpiredException;
import com.aviasales.booking.booking.exception.BookingNotFoundException;
import com.aviasales.booking.booking.exception.PaymentNotFoundException;
import com.aviasales.booking.booking.exception.PaymentProcessingException;
import com.aviasales.booking.booking.mapper.PaymentMapper;
import com.aviasales.booking.booking.repo.BookingRepository;
import com.aviasales.booking.booking.repo.PaymentRepository;
import com.aviasales.booking.booking.service.interfaces.NotificationService;
import com.aviasales.booking.booking.service.interfaces.PaymentService;
import com.aviasales.booking.booking.service.interfaces.ReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentMapper paymentMapper;
    private final NotificationService notificationService;
    private final ReceiptService receiptService;

    // ═══════════════════════════════════════
    // REGEX PATTERNS ДЛЯ ВАЛИДАЦИИ
    // ═══════════════════════════════════════

    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^\\d{13,19}$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^\\d{3,4}$");

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, Long userId) {
        log.info("Creating payment for booking: {}", request.getBookingReference());

        Booking booking = bookingRepository.findByBookingReference(request.getBookingReference())
                .orElseThrow(() -> new BookingNotFoundException(request.getBookingReference(), true));

        // Проверяем доступ
        if (!booking.getUser().getId().equals(userId)) {
            throw new BookingNotFoundException("Booking not found or access denied");
        }

        // ✅ ШАГ 1: Идемпотентность (если передан ключ)
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Payment> existingPayment = paymentRepository
                    .findByIdempotencyKey(request.getIdempotencyKey());

            if (existingPayment.isPresent()) {
                log.info("Idempotent request detected. Returning existing payment: {}",
                        existingPayment.get().getTransactionId());
                return paymentMapper.toPaymentResponse(existingPayment.get());
            }
        }

        // ✅ ШАГ 2: Проверка существующих платежей
        validateNoConflictingPayments(booking.getId());

        // ✅ ШАГ 3: Проверка лимита неудачных попыток
        validatePaymentAttempts(booking.getId());

        // Проверяем статус бронирования
        if (booking.getStatus() != BookingStatus.PENDING &&
                booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new PaymentProcessingException(
                    "Cannot process payment for booking with status: " + booking.getStatus()
            );
        }

        // Проверяем срок действия
        if (booking.isExpired()) {
            throw new BookingExpiredException(request.getBookingReference());
        }

        // ✅ НОВАЯ ЛОГИКА: Проверка суммы платежа
        BigDecimal bookingAmount = booking.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal requestAmount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal changeAmount = BigDecimal.ZERO;

        // Если заплатили меньше - ошибка
        if (requestAmount.compareTo(bookingAmount) < 0) {
            throw new PaymentProcessingException(
                    String.format("Insufficient payment amount. Required: $%.2f, Provided: $%.2f",
                            bookingAmount, requestAmount)
            );
        }

        // Если заплатили больше - вычисляем сдачу
        if (requestAmount.compareTo(bookingAmount) > 0) {
            changeAmount = requestAmount.subtract(bookingAmount);
            log.info("Overpayment detected. Booking: $%.2f, Paid: $%.2f, Change: $%.2f",
                    bookingAmount, requestAmount, changeAmount);
        }

        // Валидация данных карты
        validateCardDetails(request.getCardNumber(), request.getExpiryDate(), request.getCvv());

        // Генерируем ID транзакции
        String transactionId = generateTransactionId();
        String cardLastFour = extractCardLastFour(request.getCardNumber());

        // Создаём платёж
        Payment payment = Payment.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .amount(requestAmount)  // Полная сумма которую заплатили
                .currency("USD")
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PROCESSING)
                .transactionId(transactionId)
                .cardLastFour(cardLastFour)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment created with transaction ID: {}", transactionId);

        // Обрабатываем платёж (mock)
        boolean paymentSuccess = processPaymentMock(request);

        if (paymentSuccess) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setProcessedAt(Instant.now());

            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setConfirmedAt(LocalDateTime.now());
            booking.setPaymentStatus(PaymentStatus.PAID);
            booking.setPaymentMethod(request.getPaymentMethod());
            booking.setPaidAmount(bookingAmount);  // ✅ Только нужная сумма
            booking.setExpiresAt(LocalDateTime.now().plusYears(100));

            bookingRepository.save(booking);
            payment = paymentRepository.save(payment);

            // Создаём чек
            receiptService.createReceipt(payment, booking);

            // ✅ Если есть сдача - создаём возврат
            if (changeAmount.compareTo(BigDecimal.ZERO) > 0) {
                processRefund(payment, booking, changeAmount, "Overpayment refund");
                log.info("Refund processed for overpayment: ${}", changeAmount);
            }

            log.info("Payment completed successfully for booking: {}", booking.getBookingReference());
            notificationService.notifyPaymentSuccess(payment);

        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment declined by card issuer");
            payment.setProcessedAt(Instant.now());

            log.warn("Payment failed for booking: {}", booking.getBookingReference());
            notificationService.notifyPaymentFailed(payment);
        }

        payment = paymentRepository.save(payment);

        // ✅ Добавляем информацию о сдаче в ответ
        PaymentResponse response = paymentMapper.toPaymentResponse(payment);
        response.setChangeAmount(changeAmount);  // Добавьте это поле в PaymentResponse
        response.setMessage(changeAmount.compareTo(BigDecimal.ZERO) > 0
                ? String.format("Payment successful. Refund of $%.2f will be processed within 3-5 business days.", changeAmount)
                : "Payment successful");

        return response;
    }

    /**
     * ✅ НОВЫЙ МЕТОД: Обработка возврата переплаты
     */
    private void processRefund(Payment originalPayment, Booking booking, BigDecimal refundAmount, String reason) {
        log.info("Processing refund for payment {}: ${}", originalPayment.getTransactionId(), refundAmount);

        Payment refundPayment = Payment.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .amount(refundAmount.negate())  // Отрицательная сумма для возврата
                .currency("USD")
                .paymentMethod(originalPayment.getPaymentMethod())
                .status(PaymentStatus.REFUNDED)
                .transactionId(generateTransactionId())
                .cardLastFour(originalPayment.getCardLastFour())
                .processedAt(Instant.now())
                .failureReason(reason)  // Используем это поле для причины возврата
                .build();

        paymentRepository.save(refundPayment);

        log.info("Refund payment created with transaction ID: {}", refundPayment.getTransactionId());
    }

    // ═══════════════════════════════════════
    // ВАЛИДАЦИЯ ПЛАТЕЖЕЙ
    // ═══════════════════════════════════════

    /**
     * ✅ Проверяет что нет конфликтующих платежей
     */
    private void validateNoConflictingPayments(Long bookingId) {
        // 1. Проверяем COMPLETED платежи
        Optional<Payment> completedPayment = paymentRepository
                .findFirstByBookingIdAndStatus(bookingId, PaymentStatus.COMPLETED);

        if (completedPayment.isPresent()) {
            log.warn("Booking {} already has completed payment: {}",
                    bookingId, completedPayment.get().getTransactionId());
            throw new PaymentProcessingException(
                    "This booking has already been paid. Transaction ID: " +
                            completedPayment.get().getTransactionId()
            );
        }

        // 2. Проверяем PROCESSING платежи
        Optional<Payment> processingPayment = paymentRepository
                .findFirstByBookingIdAndStatus(bookingId, PaymentStatus.PROCESSING);

        if (processingPayment.isPresent()) {
            // Проверяем не застрял ли платёж (> 5 минут в PROCESSING)
            Instant createdAt = processingPayment.get().getCreatedAt();
            Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);

            if (createdAt.isBefore(fiveMinutesAgo)) {
                // Платёж застрял - помечаем как FAILED
                log.warn("Processing payment {} is stuck (> 5 min). Marking as FAILED",
                        processingPayment.get().getTransactionId());

                Payment stuck = processingPayment.get();
                stuck.setStatus(PaymentStatus.FAILED);
                stuck.setFailureReason("Payment timeout - processing took too long");
                stuck.setProcessedAt(Instant.now());
                paymentRepository.save(stuck);

                // Разрешаем создать новый платёж
                return;
            }

            log.warn("Booking {} already has payment in progress: {}",
                    bookingId, processingPayment.get().getTransactionId());
            throw new PaymentProcessingException(
                    "Payment is already being processed. Please wait or try again in a few minutes."
            );
        }
    }

    /**
     * ✅ Проверяет лимит неудачных попыток оплаты (защита от спама)
     */
    private void validatePaymentAttempts(Long bookingId) {
        long MAX_FAILED_ATTEMPTS = 8;
        long COOLDOWN_MINUTES = 25;

        List<Payment> allPayments = paymentRepository
                .findByBookingIdOrderByCreatedAtDesc(bookingId);

        // Считаем неудачные попытки
        long failedCount = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.FAILED)
                .count();

        if (failedCount >= MAX_FAILED_ATTEMPTS) {
            // Проверяем последнюю неудачную попытку
            Optional<Payment> lastFailedPayment = allPayments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.FAILED)
                    .findFirst();

            if (lastFailedPayment.isPresent()) {
                Instant lastAttempt = lastFailedPayment.get().getCreatedAt();
                Instant cooldownExpiry = lastAttempt.plus(COOLDOWN_MINUTES, ChronoUnit.MINUTES);

                if (Instant.now().isBefore(cooldownExpiry)) {
                    log.warn("Booking {} has {} failed attempts. Still in cooldown period",
                            bookingId, failedCount);
                    throw new PaymentProcessingException(
                            String.format("Too many failed payment attempts (%d). " +
                                            "Please try again after %d minutes or contact support.",
                                    failedCount, COOLDOWN_MINUTES)
                    );
                } else {
                    log.info("Booking {} cooldown period expired. Allowing new attempt", bookingId);
                }
            }
        }
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(ConfirmPaymentRequest request, Long userId) {
        log.info("Confirming payment with transaction ID: {}", request.getTransactionId());

        Payment payment = paymentRepository.findByTransactionId(request.getTransactionId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with transaction ID: " + request.getTransactionId()
                ));

        Booking booking = bookingRepository.findById(payment.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        // Проверяем доступ
        if (!booking.getUser().getId().equals(userId)) {
            throw new PaymentNotFoundException("Payment not found or access denied");
        }

        // Проверяем статус платежа
        if (payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new PaymentProcessingException("Payment is not in PROCESSING status");
        }

        // Подтверждаем платёж
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProcessedAt(Instant.now());

        // Подтверждаем бронирование
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setPaidAmount(payment.getAmount());
        booking.setExpiresAt(LocalDateTime.now().plusYears(100));

        bookingRepository.save(booking);
        payment = paymentRepository.save(payment);

        // Создаём чек после подтверждения платежа
        receiptService.createReceipt(payment, booking);

        log.info("Payment confirmed for booking: {}", booking.getBookingReference());
        notificationService.notifyPaymentSuccess(payment);

        return paymentMapper.toPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(String transactionId) {
        log.info("Getting payment status for transaction: {}", transactionId);

        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with transaction ID: " + transactionId
                ));

        return paymentMapper.toPaymentStatusResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingReference(String bookingReference) {
        log.info("Getting payment for booking: {}", bookingReference);

        Payment payment = paymentRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for booking: " + bookingReference
                ));

        return paymentMapper.toPaymentResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(Long paymentId, Long userId) {
        log.info("Refunding payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        Booking booking = bookingRepository.findById(payment.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        // Проверяем доступ
        if (!booking.getUser().getId().equals(userId)) {
            throw new PaymentNotFoundException("Payment not found or access denied");
        }

        // Проверяем статус платежа
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentProcessingException("Can only refund completed payments");
        }

        // Возвращаем платёж
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setProcessedAt(Instant.now());

        // Отменяем бронирование
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        bookingRepository.save(booking);

        payment = paymentRepository.save(payment);
        log.info("Payment refunded: {}", paymentId);

        return paymentMapper.toPaymentResponse(payment);
    }

    // ═══════════════════════════════════════
    // ВАЛИДАЦИЯ ДАННЫХ КАРТЫ
    // ═══════════════════════════════════════

    /**
     * ✅ Валидирует номер карты, срок действия и CVV
     * Поддерживает: UZCARD, HUMO, Visa, MasterCard, Maestro, МИР, AmEx, UnionPay
     */
    private void validateCardDetails(String cardNumber, String expiryDate, String cvv) {
        log.debug("Validating card details");

        // Валидация номера карты
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            throw new PaymentProcessingException("Card number is required");
        }

        String cleanCardNumber = cardNumber.replaceAll("\\s+", "");

        if (!CARD_NUMBER_PATTERN.matcher(cleanCardNumber).matches()) {
            throw new PaymentProcessingException(
                    "Invalid card number format. Must be 13-19 digits"
            );
        }

        // ✅ Определяем тип карты
        CardType cardType = CardType.detectCardType(cleanCardNumber);

        if (cardType == CardType.UNKNOWN) {
            throw new PaymentProcessingException("Unsupported card type");
        }

        log.debug("Detected card type: {}", cardType.getDisplayName());

        // ✅ Луна только для Visa/MasterCard/Maestro/AmEx
        // UZCARD и HUMO имеют свою систему валидации
        if (cardType.requiresLuhnValidation() && !isValidLuhn(cleanCardNumber)) {
            throw new PaymentProcessingException(
                    "Invalid " + cardType.getDisplayName() + " card number"
            );
        }

        // Валидация длины номера карты для конкретного типа
        if (cleanCardNumber.length() < cardType.getMinLength() ||
                cleanCardNumber.length() > cardType.getMaxLength()) {
            throw new PaymentProcessingException(
                    String.format("%s card must be %d digits",
                            cardType.getDisplayName(),
                            cardType.getMinLength())
            );
        }

        // Валидация срока действия
        if (expiryDate == null || expiryDate.trim().isEmpty()) {
            throw new PaymentProcessingException("Expiry date is required");
        }

        if (!isValidExpiryDate(expiryDate)) {
            throw new PaymentProcessingException(
                    "Invalid or expired card. Format: MM/YY"
            );
        }

        // ✅ Валидация CVV с учётом типа карты
        if (cvv == null || cvv.trim().isEmpty()) {
            throw new PaymentProcessingException("CVV is required");
        }

        int expectedCvvLength = cardType.getExpectedCvvLength();
        if (cvv.length() != expectedCvvLength) {
            throw new PaymentProcessingException(
                    String.format("CVV for %s must be %d digits",
                            cardType.getDisplayName(),
                            expectedCvvLength)
            );
        }

        if (!CVV_PATTERN.matcher(cvv).matches()) {
            throw new PaymentProcessingException(
                    "Invalid CVV format. Must contain only digits"
            );
        }

        log.info("Card validation successful: {} ending in {}",
                cardType.getDisplayName(),
                cleanCardNumber.substring(cleanCardNumber.length() - 4));
    }

    /**
     * Проверка номера карты по алгоритму Луна
     * Применяется только для Visa, MasterCard, Maestro, AmEx
     */
    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (sum % 10 == 0);
    }

    /**
     * Проверка срока действия карты (MM/YY или MM/YYYY)
     */
    private boolean isValidExpiryDate(String expiryDate) {
        try {
            String cleanExpiry = expiryDate.trim();

            // Проверяем формат MM/YY или MM/YYYY
            if (!cleanExpiry.matches("^(0[1-9]|1[0-2])/\\d{2,4}$")) {
                return false;
            }

            // Парсим дату
            String[] parts = cleanExpiry.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);

            // Если год 2-х значный, добавляем 2000
            if (year < 100) {
                year += 2000;
            }

            YearMonth cardExpiry = YearMonth.of(year, month);
            YearMonth currentMonth = YearMonth.now();

            // Карта действительна если срок >= текущий месяц
            return cardExpiry.compareTo(currentMonth) >= 0;

        } catch (DateTimeParseException | NumberFormatException e) {
            log.warn("Failed to parse expiry date: {}", expiryDate, e);
            return false;
        }
    }

    // ═══════════════════════════════════════
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ═══════════════════════════════════════

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String extractCardLastFour(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String cleanNumber = cardNumber.replaceAll("\\s+", "");
        return cleanNumber.substring(cleanNumber.length() - 4);
    }

    /**
     * Mock обработка платежа
     * В продакшене здесь будет интеграция с платёжным шлюзом:
     * - Для UZCARD/HUMO: интеграция с Payme, Click, Uzum
     * - Для Visa/MasterCard: Stripe, PayPal, Square
     */
    private boolean processPaymentMock(CreatePaymentRequest request) {
        log.info("Processing payment (MOCK) for method: {}", request.getPaymentMethod());

        // Имитация задержки обработки
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Карты заканчивающиеся на 0000 будут отклонены (для тестирования)
        if (request.getCardNumber() != null && request.getCardNumber().endsWith("0000")) {
            log.warn("Card ending with 0000 - payment declined");
            return false;
        }

        return true;
    }
}
