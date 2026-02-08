package com.aviasales.booking.booking.service.interfaces;

import com.aviasales.booking.booking.entity.Booking;
import com.aviasales.booking.booking.entity.Payment;

public interface EmailService {

    void sendBookingConfirmation(Booking booking, String userEmail);

    void sendPaymentSuccess(Payment payment, String userEmail);

    void sendBookingReminder(Booking booking, String userEmail);

    void sendBookingCancellation(Booking booking, String userEmail);
}
