package com.aviasales.booking.booking.exception;

public class NoAvailableSeatsException extends RuntimeException {

    public NoAvailableSeatsException(String message) {
        super(message);
    }

    public NoAvailableSeatsException(Long flightId, int requested, int available) {
        super(String.format("Flight %d has only %d available seats, but %d were requested",
                flightId, available, requested));
    }
}
