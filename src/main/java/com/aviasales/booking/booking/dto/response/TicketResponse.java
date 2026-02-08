package com.aviasales.booking.booking.dto.response;

import com.aviasales.booking.booking.enums.CabinClass;
import com.aviasales.booking.booking.enums.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ticket information")
public class TicketResponse {

    @Schema(description = "Ticket ID")
    private Long id;

    @Schema(description = "Ticket number")
    private String ticketNumber;

    @Schema(description = "E-ticket number")
    private String eTicketNumber;

    @Schema(description = "Passenger full name")
    private String passengerName;

    @Schema(description = "Passenger first name")
    private String passengerFirstName;

    @Schema(description = "Passenger last name")
    private String passengerLastName;

    @Schema(description = "Flight number")
    private String flightNumber;

    @Schema(description = "Origin airport code")
    private String origin;

    @Schema(description = "Destination airport code")
    private String destination;

    @Schema(description = "Departure time")
    private Instant departureTime;

    @Schema(description = "Arrival time")
    private Instant arrivalTime;

    @Schema(description = "Cabin class")
    private CabinClass cabinClass;

    @Schema(description = "Seat number")
    private String seatNumber;

    @Schema(description = "Ticket price")
    private BigDecimal price;

    @Schema(description = "Ticket status")
    private TicketStatus status;

    @Schema(description = "Is refundable")
    private Boolean isRefundable;

    @Schema(description = "Checked baggage allowance (kg)")
    private Integer checkedBaggage;

    @Schema(description = "Hand luggage allowance (kg)")
    private Integer handLuggage;
}
