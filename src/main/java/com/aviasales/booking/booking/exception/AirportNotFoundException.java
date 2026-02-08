package com.aviasales.booking.booking.exception;

public class AirportNotFoundException extends RuntimeException {

    public AirportNotFoundException(String message) {
        super(message);
    }

    public AirportNotFoundException(String iataCode, boolean isCode) {
        super("Airport not found with IATA code: " + iataCode);
    }
}
