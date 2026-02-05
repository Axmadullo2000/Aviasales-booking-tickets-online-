package com.monolit.booking.booking.service.interfaces;

import com.monolit.booking.booking.entity.Booking;
import com.monolit.booking.booking.entity.Payment;

public interface NotificationService {

    void notifyBookingCreated(Booking booking);

    void notifyBookingConfirmed(Booking booking);

    void notifyBookingCancelled(Booking booking);

    void notifyPaymentSuccess(Payment payment);

    void notifyPaymentFailed(Payment payment);
}
