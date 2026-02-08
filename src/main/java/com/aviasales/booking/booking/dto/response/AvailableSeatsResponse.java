package com.aviasales.booking.booking.dto.response;

import com.aviasales.booking.booking.enums.CabinClass;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Set;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Available seats information")
public class AvailableSeatsResponse {

    @Schema(description = "Flight ID", example = "36")
    private Long flightId;

    @Schema(description = "Flight number", example = "HY404")
    private String flightNumber;

    @Schema(description = "Cabin class", example = "BUSINESS")
    private CabinClass cabinClass;

    @Schema(description = "Set of occupied seats", example = "[\"5A\", \"5B\", \"6C\"]")
    private Set<String> occupiedSeats;

    @Schema(description = "Total seats in this class", example = "36")
    private Integer totalSeats;

    @Schema(description = "Available seats count", example = "33")
    private Integer availableSeats;

}
