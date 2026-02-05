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
public class UpdateFlightRequest {

    @Future(message = "Departure time must be in the future")
    private OffsetDateTime departureTime;

    @Future(message = "Arrival time must be in the future")
    private OffsetDateTime arrivalTime;

    @Min(value = 1, message = "Total seats must be at least 1")
    private Integer totalSeats;

    @DecimalMin(value = "0.01", message = "Economy price must be greater than 0")
    private BigDecimal priceEconomy;

    @DecimalMin(value = "0.01", message = "Business price must be greater than 0")
    private BigDecimal priceBusiness;

    private FlightStatus status;
}
