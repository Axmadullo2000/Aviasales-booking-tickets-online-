package com.monolit.booking.booking.exception;

public class BookingExpiredException extends RuntimeException {

    public BookingExpiredException(String message) {
        super(message);
    }

    public static BookingExpiredException forReference(String bookingReference) {
        return new BookingExpiredException("Booking with reference " + bookingReference + " has expired");
    }
}
