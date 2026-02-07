package com.monolit.booking.booking.service.impl;

import com.monolit.booking.booking.entity.Booking;
import com.monolit.booking.booking.entity.Payment;
import com.monolit.booking.booking.service.interfaces.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Override
    public void sendBookingConfirmation(Booking booking, String userEmail) {
        log.info("=== MOCK EMAIL: Booking Confirmation ===");
        log.info("To: {}", userEmail);
        log.info("Subject: Booking Confirmation - {}", booking.getBookingReference());
        log.info("Body:");
        log.info("  Dear Customer,");
        log.info("  Your booking has been confirmed!");
        log.info("  Booking Reference: {}", booking.getBookingReference());
        log.info("  Status: {}", booking.getStatus());
        log.info("  Thank you for choosing our service!");
        log.info("=========================================");
    }

    @Override
    public void sendPaymentSuccess(Payment payment, String userEmail) {
        log.info("=== MOCK EMAIL: Payment Success ===");
        log.info("To: {}", userEmail);
        log.info("Subject: Payment Successful - {}", payment.getBookingReference());
        log.info("Body:");
        log.info("  Dear Customer,");
        log.info("  Your payment has been processed successfully!");
        log.info("  Transaction ID: {}", payment.getTransactionId());
        log.info("  Amount: ${} {}", payment.getAmount(), payment.getCurrency());
        log.info("  Payment Method: {}", payment.getPaymentMethod());
        log.info("  Card: ****{}", payment.getCardLastFour());
        log.info("  Your booking {} is now confirmed.", payment.getBookingReference());
        log.info("====================================");
    }

    @Override
    public void sendBookingReminder(Booking booking, String userEmail) {
        log.info("=== MOCK EMAIL: Booking Reminder ===");
        log.info("To: {}", userEmail);
        log.info("Subject: Booking Reminder - {}", booking.getBookingReference());
        log.info("Body:");
        log.info("  Dear Customer,");
        log.info("  This is a reminder about your upcoming flight.");
        log.info("  Booking Reference: {}", booking.getBookingReference());
        log.info("  Please arrive at the airport at least 2 hours before departure.");
        log.info("=====================================");
    }

    @Override
    public void sendBookingCancellation(Booking booking, String userEmail) {
        log.info("=== MOCK EMAIL: Booking Cancellation ===");
        log.info("To: {}", userEmail);
        log.info("Subject: Booking Cancelled - {}", booking.getBookingReference());
        log.info("Body:");
        log.info("  Dear Customer,");
        log.info("  Your booking has been cancelled.");
        log.info("  Booking Reference: {}", booking.getBookingReference());
        log.info("  If you did not request this cancellation, please contact support.");
        log.info("=========================================");
    }
}
