package com.monolit.booking.booking.exception;

public class InsufficientSeatsException extends Exception {
    public InsufficientSeatsException(String message) {
        super(message);
    }
}
