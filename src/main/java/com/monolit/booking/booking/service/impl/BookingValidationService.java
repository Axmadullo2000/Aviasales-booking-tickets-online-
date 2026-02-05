package com.monolit.booking.booking.service.impl;

import com.monolit.booking.booking.dto.request.BookingFlightRequest;
import com.monolit.booking.booking.dto.request.CreateBookingRequest;
import com.monolit.booking.booking.entity.Flight;
import com.monolit.booking.booking.exception.*;
import com.monolit.booking.booking.repo.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingValidationService {

    private final FlightRepository flightRepository;

    public void validateBookingRequest(CreateBookingRequest request) {
        if (request.getFlights() == null || request.getFlights().isEmpty()) {
            throw new IllegalArgumentException("At least one flight is required");
        }

        if (request.getPassengers() == null || request.getPassengers().isEmpty()) {
            throw new IllegalArgumentException("At least one passenger is required");
        }

        int passengerCount = request.getPassengers().size();

        for (BookingFlightRequest flightRequest : request.getFlights()) {
            Flight flight = flightRepository.findById(flightRequest.getFlightId())
                    .orElseThrow(() -> new FlightNotFoundException(flightRequest.getFlightId()));

            if (!flight.hasAvailableSeats(passengerCount)) {
                throw new NoAvailableSeatsException(
                        flight.getId(),
                        passengerCount,
                        flight.getAvailableSeats()
                );
            }
        }
    }

    public Flight getAndValidateFlight(Long flightId, int passengerCount) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new FlightNotFoundException(flightId));

        if (!flight.hasAvailableSeats(passengerCount)) {
            throw new NoAvailableSeatsException(
                    flight.getId(),
                    passengerCount,
                    flight.getAvailableSeats()
            );
        }

        return flight;
    }
}
