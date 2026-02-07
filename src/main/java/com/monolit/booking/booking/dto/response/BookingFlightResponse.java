package com.monolit.booking.booking.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingFlightResponse {

    private FlightResponse flight;  // информация о рейсе

    // ✅ Данные из Ticket
    private String seatNumber;      // место пассажира
    private BigDecimal price;       // цена билета
    private String ticketNumber;    // номер билета
    private String cabinClass;      // класс обслуживания
}