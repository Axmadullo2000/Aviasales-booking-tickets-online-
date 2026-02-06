package com.monolit.booking.booking.dto.response;

import com.monolit.booking.booking.enums.FlightStatus;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightDetailResponse implements Serializable {

    private Long id;
    private String flightNumber;
    private AirlineResponse airline;
    private AirportResponse departureAirport;
    private AirportResponse arrivalAirport;
    private Instant departureTime;
    private Instant arrivalTime;
    private Integer durationMinutes;
    private Integer totalSeats;
    private Integer availableSeats;
    private BigDecimal priceEconomy;
    private BigDecimal priceBusiness;
    private FlightStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
