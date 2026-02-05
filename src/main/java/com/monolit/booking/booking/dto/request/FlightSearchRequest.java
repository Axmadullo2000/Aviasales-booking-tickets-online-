package com.monolit.booking.booking.dto.request;

import com.monolit.booking.booking.enums.FlightSortBy;
import com.monolit.booking.booking.enums.SeatClass;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightSearchRequest {

    @NotBlank(message = "Departure airport is required")
    @Size(min = 3, max = 3, message = "Airport code must be 3 characters")
    private String departureAirport;

    @NotBlank(message = "Arrival airport is required")
    @Size(min = 3, max = 3, message = "Airport code must be 3 characters")
    private String arrivalAirport;

    @NotNull(message = "Departure date is required")
    @FutureOrPresent(message = "Departure date must be today or in the future")
    private LocalDate departureDate;

    @Min(value = 1, message = "At least 1 passenger is required")
    @Max(value = 9, message = "Maximum 9 passengers allowed")
    private Integer passengers = 1;

    private SeatClass seatClass = SeatClass.ECONOMY;

    private FlightSortBy sortBy = FlightSortBy.PRICE;
}
