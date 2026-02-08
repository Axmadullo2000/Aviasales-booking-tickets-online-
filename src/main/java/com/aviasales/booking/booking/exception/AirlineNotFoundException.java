package com.aviasales.booking.booking.exception;

public class AirlineNotFoundException extends RuntimeException {

    public AirlineNotFoundException(String message) {
        super(message);
    }

    public AirlineNotFoundException(String iataCode, boolean isCode) {
        super("Airline not found with IATA code: " + iataCode);
    }
}
