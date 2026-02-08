package com.aviasales.booking.booking.exception;

public class FlightNotFoundException extends RuntimeException {

    public FlightNotFoundException(String message) {
        super(message);
    }

    public FlightNotFoundException(Long id) {
        super("Flight not found with id: " + id);
    }
}
