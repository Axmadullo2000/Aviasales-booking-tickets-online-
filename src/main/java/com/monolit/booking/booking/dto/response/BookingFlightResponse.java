package com.monolit.booking.booking.dto.response;

import com.monolit.booking.booking.enums.SeatClass;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingFlightResponse {

    private Long id;
    private FlightDetailResponse flight;
    private Integer passengerCount;
    private SeatClass seatClass;
    private BigDecimal pricePerSeat;
    private BigDecimal totalPrice;
}
