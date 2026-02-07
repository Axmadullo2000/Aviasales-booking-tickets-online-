package com.monolit.booking.booking.service.impl;

import com.monolit.booking.booking.dto.request.*;
import com.monolit.booking.booking.dto.response.*;
import com.monolit.booking.booking.entity.*;
import com.monolit.booking.booking.enums.*;
import com.monolit.booking.booking.exception.*;
import com.monolit.booking.booking.mapper.PaymentMapper;
import com.monolit.booking.booking.repo.*;
import com.monolit.booking.booking.service.interfaces.NotificationService;
import com.monolit.booking.booking.service.interfaces.PaymentService;
import com.monolit.booking.booking.service.interfaces.ReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentMapper paymentMapper;
    private final NotificationService notificationService;
    private final ReceiptService receiptService;

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

        // Проверяем статус бронирования
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new PaymentProcessingException("Cannot process payment for booking with status: " + booking.getStatus());
        }

        // Проверяем срок действия
        if (booking.isExpired()) {
            throw new BookingExpiredException(request.getBookingReference());
        }

        // Проверяем сумму платежа
        if (!booking.getTotalAmount().equals(request.getAmount())) {
            throw new PaymentProcessingException("Payment amount does not match booking total");
        }

        // Генерируем ID транзакции
        String transactionId = generateTransactionId();
        String cardLastFour = extractCardLastFour(request.getCardNumber());

        // Создаём платёж
        Payment payment = Payment.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .amount(request.getAmount())
                .currency("USD")
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PROCESSING)
                .transactionId(transactionId)
                .cardLastFour(cardLastFour)
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

            // ✅ ИСПРАВЛЕНО: устанавливаем expires_at в далёкое будущее вместо null
            // Подтверждённые бронирования не истекают, поэтому ставим +100 лет
            booking.setExpiresAt(LocalDateTime.now().plusYears(100));

            bookingRepository.save(booking);

            // Создаём чек после успешной оплаты
            payment = paymentRepository.save(payment);
            receiptService.createReceipt(payment, booking);

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
        return paymentMapper.toPaymentResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(ConfirmPaymentRequest request, Long userId) {
        log.info("Confirming payment with transaction ID: {}", request.getTransactionId());

        Payment payment = paymentRepository.findByTransactionId(request.getTransactionId())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with transaction ID: " + request.getTransactionId()));

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

        // ✅ ИСПРАВЛЕНО: устанавливаем expires_at в далёкое будущее вместо null
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
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with transaction ID: " + transactionId));

        return paymentMapper.toPaymentStatusResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingReference(String bookingReference) {
        log.info("Getting payment for booking: {}", bookingReference);

        Payment payment = paymentRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for booking: " + bookingReference));

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
        bookingRepository.save(booking);

        payment = paymentRepository.save(payment);
        log.info("Payment refunded: {}", paymentId);

        return paymentMapper.toPaymentResponse(payment);
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
        return cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * Mock обработка платежа
     * В продакшене здесь будет интеграция с платёжным шлюзом
     */
    private boolean processPaymentMock(CreatePaymentRequest request) {
        log.info("Processing payment (MOCK) for method: {}", request.getPaymentMethod());

        // Имитация задержки обработки
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Карты заканчивающиеся на 0000 будут отклонены
        return request.getCardNumber() == null || !request.getCardNumber().endsWith("0000");
    }
}
