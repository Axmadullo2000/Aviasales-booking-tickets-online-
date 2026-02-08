package com.aviasales.booking.booking.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightDetailResponse {

    private FlightResponse flight;

    private Integer totalSeats;
    private Integer economySeats;
    private Integer businessSeats;
    private Integer firstClassSeats;

    private SeatAvailability seatAvailability;

    private AirportResponse connectionAirport;
}
