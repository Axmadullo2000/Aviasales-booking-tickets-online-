package com.monolit.booking.booking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotEmpty(message = "At least one flight is required")
    @Valid
    private List<BookingFlightRequest> flights;

    @NotEmpty(message = "At least one passenger is required")
    @Valid
    private List<PassengerInfoRequest> passengers;
}
