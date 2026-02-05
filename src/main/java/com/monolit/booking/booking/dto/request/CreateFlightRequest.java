package com.monolit.booking.booking.dto.request;

import com.monolit.booking.booking.enums.FlightStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFlightRequest {

    @NotBlank(message = "Flight number is required")
    private String flightNumber;

    @NotBlank(message = "Airline IATA code is required")
    @Size(min = 2, max = 2, message = "Airline code must be 2 characters")
    private String airlineCode;

    @NotBlank(message = "Departure airport is required")
    @Size(min = 3, max = 3, message = "Airport code must be 3 characters")
    private String departureAirportCode;

    @NotBlank(message = "Arrival airport is required")
    @Size(min = 3, max = 3, message = "Airport code must be 3 characters")
    private String arrivalAirportCode;

    @NotNull(message = "Departure time is required")
    @Future(message = "Departure time must be in the future")
    private OffsetDateTime departureTime;

    @NotNull(message = "Arrival time is required")
    @Future(message = "Arrival time must be in the future")
    private OffsetDateTime arrivalTime;

    @NotNull(message = "Total seats is required")
    @Min(value = 1, message = "Total seats must be at least 1")
    private Integer totalSeats;

    @NotNull(message = "Economy price is required")
    @DecimalMin(value = "0.01", message = "Economy price must be greater than 0")
    private BigDecimal priceEconomy;

    @NotNull(message = "Business price is required")
    @DecimalMin(value = "0.01", message = "Business price must be greater than 0")
    private BigDecimal priceBusiness;

    private FlightStatus status = FlightStatus.SCHEDULED;
}
