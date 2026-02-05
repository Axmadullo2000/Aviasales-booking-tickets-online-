package com.monolit.booking.booking.service.interfaces;

import com.monolit.booking.booking.entity.Booking;
import com.monolit.booking.booking.entity.Payment;

public interface EmailService {

    void sendBookingConfirmation(Booking booking, String userEmail);

    void sendPaymentSuccess(Payment payment, String userEmail);

    void sendBookingReminder(Booking booking, String userEmail);

    void sendBookingCancellation(Booking booking, String userEmail);
}
