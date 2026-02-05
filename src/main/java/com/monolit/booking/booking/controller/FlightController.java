package com.monolit.booking.booking.controller;

import com.monolit.booking.booking.dto.request.*;
import com.monolit.booking.booking.dto.response.*;
import com.monolit.booking.booking.enums.FlightSortBy;
import com.monolit.booking.booking.enums.SeatClass;
import com.monolit.booking.booking.service.interfaces.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
@Tag(name = "Flights", description = "Flight search and management endpoints")
public class FlightController {

    private final FlightService flightService;

    @GetMapping("/search")
    @Operation(summary = "Search for flights", description = "Search available flights by route, date, and passengers")
    public ResponseEntity<Page<FlightSearchResponse>> searchFlights(
            @Parameter(description = "Departure airport IATA code (e.g., TAS)")
            @RequestParam String from,
            @Parameter(description = "Arrival airport IATA code (e.g., MOW)")
            @RequestParam String to,
            @Parameter(description = "Departure date (YYYY-MM-DD)")
            @RequestParam LocalDate date,
            @Parameter(description = "Number of passengers")
            @RequestParam(defaultValue = "1") Integer passengers,
            @Parameter(description = "Seat class")
            @RequestParam(defaultValue = "ECONOMY") SeatClass seatClass,
            @Parameter(description = "Sort by")
            @RequestParam(defaultValue = "PRICE") FlightSortBy sortBy,
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size
    ) {
        FlightSearchRequest request = FlightSearchRequest.builder()
                .departureAirport(from)
                .arrivalAirport(to)
                .departureDate(date)
                .passengers(passengers)
                .seatClass(seatClass)
                .sortBy(sortBy)
                .build();

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(flightService.searchFlights(request, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get flight by ID", description = "Get detailed information about a specific flight")
    public ResponseEntity<FlightDetailResponse> getFlightById(
            @Parameter(description = "Flight ID")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(flightService.getFlightById(id));
    }

    @GetMapping("/number/{flightNumber}")
    @Operation(summary = "Get flight by number", description = "Get detailed information about a flight by its flight number")
    public ResponseEntity<FlightDetailResponse> getFlightByNumber(
            @Parameter(description = "Flight number (e.g., HY501)")
            @PathVariable String flightNumber
    ) {
        return ResponseEntity.ok(flightService.getFlightByNumber(flightNumber));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Create a new flight", description = "Create a new flight (Admin/Manager only)")
    public ResponseEntity<FlightDetailResponse> createFlight(
            @Valid @RequestBody CreateFlightRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flightService.createFlight(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Update a flight", description = "Update flight details (Admin/Manager only)")
    public ResponseEntity<FlightDetailResponse> updateFlight(
            @Parameter(description = "Flight ID")
            @PathVariable Long id,
            @Valid @RequestBody UpdateFlightRequest request
    ) {
        return ResponseEntity.ok(flightService.updateFlight(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a flight", description = "Delete a flight (Admin only)")
    public ResponseEntity<Void> deleteFlight(
            @Parameter(description = "Flight ID")
            @PathVariable Long id
    ) {
        flightService.deleteFlight(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/airports")
    @Operation(summary = "Get all airports", description = "Get list of all active airports")
    public ResponseEntity<List<AirportResponse>> getAllAirports() {
        return ResponseEntity.ok(flightService.getAllAirports());
    }

    @GetMapping("/airports/search")
    @Operation(summary = "Search airports", description = "Search airports by code, name, or city")
    public ResponseEntity<List<AirportResponse>> searchAirports(
            @Parameter(description = "Search query")
            @RequestParam String query
    ) {
        return ResponseEntity.ok(flightService.searchAirports(query));
    }

    @GetMapping("/airports/{iataCode}")
    @Operation(summary = "Get airport by code", description = "Get airport details by IATA code")
    public ResponseEntity<AirportResponse> getAirportByCode(
            @Parameter(description = "Airport IATA code (e.g., TAS)")
            @PathVariable String iataCode
    ) {
        return ResponseEntity.ok(flightService.getAirportByCode(iataCode));
    }

    @GetMapping("/airlines")
    @Operation(summary = "Get all airlines", description = "Get list of all active airlines")
    public ResponseEntity<List<AirlineResponse>> getAllAirlines() {
        return ResponseEntity.ok(flightService.getAllAirlines());
    }

    @GetMapping("/airlines/{iataCode}")
    @Operation(summary = "Get airline by code", description = "Get airline details by IATA code")
    public ResponseEntity<AirlineResponse> getAirlineByCode(
            @Parameter(description = "Airline IATA code (e.g., HY)")
            @PathVariable String iataCode
    ) {
        return ResponseEntity.ok(flightService.getAirlineByCode(iataCode));
    }

    @GetMapping("/popular-destinations")
    @Operation(summary = "Get popular destinations", description = "Get most popular destination airports")
    public ResponseEntity<List<PopularDestinationResponse>> getPopularDestinations(
            @Parameter(description = "Number of destinations to return")
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(flightService.getPopularDestinations(limit));
    }
}
