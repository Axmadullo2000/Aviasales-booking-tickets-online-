package com.aviasales.booking.booking.dto.request;

import com.aviasales.booking.booking.enums.FlightStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFlightRequest {

    private Instant departureTime;
    private Instant arrivalTime;

    private Integer totalSeats;

    private BigDecimal basePrice;
    private BigDecimal businessPrice;
    private BigDecimal firstClassPrice;

    private FlightStatus status;
}
