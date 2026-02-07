package com.monolit.booking.booking.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFlightRequest {

    @NotBlank
    @Size(max = 10)
    private String flightNumber;  // SU1234

    @NotBlank
    @Size(min = 2, max = 2)
    private String airlineCode;  // SU

    @NotBlank
    @Size(min = 3, max = 3)
    private String originCode;  // DME

    @NotBlank
    @Size(min = 3, max = 3)
    private String destinationCode;  // DXB

    @NotNull
    @Future
    private Instant departureTime;

    @NotNull
    @Future
    private Instant arrivalTime;

    @NotNull
    @Min(1)
    private Integer totalSeats;

    private Integer economySeats;
    private Integer businessSeats;
    private Integer firstClassSeats;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal basePrice;

    private BigDecimal businessPrice;
    private BigDecimal firstClassPrice;

    private String aircraftType;  // Boeing 777-300ER
}
