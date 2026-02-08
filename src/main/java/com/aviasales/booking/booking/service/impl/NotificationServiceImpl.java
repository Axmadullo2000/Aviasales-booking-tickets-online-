package com.aviasales.booking.booking.service.impl;

import com.aviasales.booking.booking.entity.Booking;
import com.aviasales.booking.booking.entity.Payment;
import com.aviasales.booking.booking.repo.BookingRepository;
import com.aviasales.booking.booking.repo.UserRepository;
import com.aviasales.booking.booking.service.interfaces.EmailService;
import com.aviasales.booking.booking.service.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Async
    public void notifyBookingCreated(Booking booking) {
        log.info("Notifying user about booking created: {}", booking.getBookingReference());
        // ✅ Получаем email напрямую из связи
        String email = booking.getUser().getEmail();
        if (email != null) {
            log.info("Booking {} created - pending payment", booking.getBookingReference());
        }
    }

    @Override
    @Async
    public void notifyBookingConfirmed(Booking booking) {
        log.info("Notifying user about booking confirmed: {}", booking.getBookingReference());
        // ✅ Получаем email напрямую из связи
        String email = booking.getUser().getEmail();
        if (email != null) {
            emailService.sendBookingConfirmation(booking, email);
        }
    }

    @Override
    @Async
    public void notifyBookingCancelled(Booking booking) {
        log.info("Notifying user about booking cancelled: {}", booking.getBookingReference());
        // ✅ Получаем email напрямую из связи
        String email = booking.getUser().getEmail();
        if (email != null) {
            emailService.sendBookingCancellation(booking, email);
        }
    }

    @Override
    @Async
    public void notifyPaymentSuccess(Payment payment) {
        log.info("Notifying user about payment success: {}", payment.getTransactionId());
        String email = getUserEmailByBookingId(payment.getBookingId());
        if (email != null) {
            emailService.sendPaymentSuccess(payment, email);
        }
    }

    @Override
    @Async
    public void notifyPaymentFailed(Payment payment) {
        log.info("Notifying user about payment failed: {}", payment.getTransactionId());
        log.warn("Payment failed for booking: {}, reason: {}",
                payment.getBookingReference(), payment.getFailureReason());
    }

    // ✅ Исправил: теперь через booking.getUser().getEmail()
    private String getUserEmailByBookingId(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .map(booking -> booking.getUser().getEmail())
                .orElse(null);
    }
}
