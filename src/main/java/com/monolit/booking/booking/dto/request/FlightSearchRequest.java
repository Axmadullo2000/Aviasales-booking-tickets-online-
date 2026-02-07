package com.monolit.booking.booking.dto.request;

import com.monolit.booking.booking.enums.CabinClass;
import com.monolit.booking.booking.enums.FlightSortBy;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightSearchRequest {

    @NotBlank(message = "Origin airport code is required")
    @Size(min = 3, max = 3, message = "Airport code must be 3 characters")
    private String originCode;  // DME

    @NotBlank(message = "Destination airport code is required")
    @Size(min = 3, max = 3, message = "Airport code must be 3 characters")
    private String destinationCode;  // DXB

    @NotNull(message = "Departure date is required")
    @FutureOrPresent(message = "Departure date must be in the future or today")
    private LocalDate departureDate;

    @NotNull(message = "Number of passengers is required")
    @Min(value = 1, message = "At least 1 passenger required")
    @Max(value = 9, message = "Maximum 9 passengers allowed")
    private Integer passengers;

    private CabinClass cabinClass;  // default ECONOMY

    private FlightSortBy sortBy;    // default PRICE
}
