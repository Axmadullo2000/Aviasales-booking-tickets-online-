package com.aviasales.booking.booking.service.interfaces;

import com.aviasales.booking.booking.entity.Booking;
import com.aviasales.booking.booking.entity.Payment;

public interface NotificationService {

    void notifyBookingCreated(Booking booking);

    void notifyBookingConfirmed(Booking booking);

    void notifyBookingCancelled(Booking booking);

    void notifyPaymentSuccess(Payment payment);

    void notifyPaymentFailed(Payment payment);
}
