package com.monolit.booking.booking.dto.request;

import com.monolit.booking.booking.enums.SeatClass;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingFlightRequest {

    @NotNull(message = "Flight ID is required")
    private Long flightId;

    @NotNull(message = "Seat class is required")
    private SeatClass seatClass;
}
