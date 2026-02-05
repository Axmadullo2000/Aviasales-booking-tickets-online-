package com.monolit.booking.booking.exception;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(String message) {
        super(message);
    }

    public BookingNotFoundException(String bookingReference, boolean isReference) {
        super("Booking not found with reference: " + bookingReference);
    }
}
