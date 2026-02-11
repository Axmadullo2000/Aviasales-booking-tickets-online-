package com.aviasales.booking.booking.controller;

import com.aviasales.booking.booking.dto.request.RoundTripSearchRequest;
import com.aviasales.booking.booking.dto.response.RoundTripDiscountResponse;
import com.aviasales.booking.booking.dto.response.RoundTripSearchResponse;
import com.aviasales.booking.booking.enums.CabinClass;
import com.aviasales.booking.booking.service.interfaces.RoundTripService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


/**
 * API для поиска билетов туда-обратно
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
@Tag(name = "Round Trip", description = "Round-trip flight search with discounts")
public class RoundTripController {

    private final RoundTripService roundTripService;

    /**
     * Поиск рейсов туда-обратно с автоматической скидкой
     */
    @PostMapping("/round-trip/search")
    @Operation(
            summary = "Search round-trip flights",
            description = "Search for round-trip flights with automatic 5% discount on return flight. " +
                    "Returns recommended combinations sorted by best value."
    )
    public ResponseEntity<RoundTripSearchResponse> searchRoundTrip(
            @Parameter(description = "Round-trip search criteria")
            @Valid @RequestBody RoundTripSearchRequest request
    ) {
        log.info("POST /api/flights/round-trip/search - route: {} -> {}, dates: {} - {}",
                request.getFrom(), request.getTo(),
                request.getDepartureDate(), request.getReturnDate());

        RoundTripSearchResponse response = roundTripService.searchRoundTrip(request);

        log.info("Found {} outbound and {} return flights, {} recommendations",
                response.getOutboundFlights().size(),
                response.getReturnFlights().size(),
                response.getRecommendations().size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/round-trip/discount")
    @Operation(
            summary = "Calculate round-trip discount",
            description = "Returns the discount amount for a specific round-trip combination"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Discount calculated successfully"),
            @ApiResponse(responseCode = "404", description = "Flight not found")
    })
    public ResponseEntity<RoundTripDiscountResponse> calculateDiscount(
            @Parameter(description = "Outbound flight ID", example = "36")
            @RequestParam Long outboundFlightId,

            @Parameter(description = "Return flight ID", example = "37")
            @RequestParam Long returnFlightId,

            @Parameter(description = "Cabin class", example = "ECONOMY")
            @RequestParam(defaultValue = "ECONOMY") CabinClass cabinClass,

            @Parameter(description = "Booking date (defaults to today)", example = "2026-02-08")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate
    ) {
        log.info("GET /api/flights/round-trip/discount - outbound={}, return={}, class={}",
                outboundFlightId, returnFlightId, cabinClass);

        LocalDate effectiveBookingDate = bookingDate != null ? bookingDate : LocalDate.now();

        RoundTripDiscountResponse response = roundTripService.calculateDiscount(
                outboundFlightId,
                returnFlightId,
                cabinClass,
                effectiveBookingDate
        );

        return ResponseEntity.ok(response);
    }
}
