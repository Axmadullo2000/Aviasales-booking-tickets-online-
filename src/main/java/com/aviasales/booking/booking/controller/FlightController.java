package com.aviasales.booking.booking.controller;

import com.aviasales.booking.booking.dto.request.CreateFlightRequest;
import com.aviasales.booking.booking.dto.request.FlightSearchRequest;
import com.aviasales.booking.booking.dto.request.UpdateFlightRequest;
import com.aviasales.booking.booking.dto.response.*;
import com.aviasales.booking.booking.enums.CabinClass;
import com.aviasales.booking.booking.enums.FlightSortBy;
import com.aviasales.booking.booking.service.interfaces.BookingService;
import com.aviasales.booking.booking.service.interfaces.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
@Tag(name = "Flights", description = "Flight search and management endpoints")
public class FlightController {

    private final FlightService flightService;
    private final BookingService bookingService;

    // ═══════════════════════════════════════
    // ПОИСК РЕЙСОВ
    // ═══════════════════════════════════════

    @GetMapping("/search")
    @Operation(
            summary = "Search for flights",
            description = "Search available flights by route, date, and number of passengers"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Flights found successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters")
    })
    public ResponseEntity<Page<FlightResponse>> searchFlights(
            @Parameter(description = "Origin airport IATA code (e.g., DME for Moscow)")
            @RequestParam String from,

            @Parameter(description = "Destination airport IATA code (e.g., DXB for Dubai)")
            @RequestParam String to,

            @Parameter(description = "Departure date (YYYY-MM-DD)")
            @RequestParam LocalDate date,

            @Parameter(description = "Number of passengers (1-9)")
            @RequestParam(defaultValue = "1") Integer passengers,

            @Parameter(description = "Cabin class preference")
            @RequestParam(defaultValue = "ECONOMY") CabinClass cabinClass,  // ✅ исправлено

            @Parameter(description = "Sort criteria")
            @RequestParam(defaultValue = "PRICE") FlightSortBy sortBy,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Flight search request: {} -> {}, date: {}, passengers: {}",
                from, to, date, passengers);

        FlightSearchRequest request = FlightSearchRequest.builder()
                .originCode(from)             // ✅ исправлено
                .destinationCode(to)          // ✅ исправлено
                .departureDate(date)
                .passengers(passengers)
                .cabinClass(cabinClass)       // ✅ исправлено
                .sortBy(sortBy)
                .build();

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(flightService.searchFlights(request, pageable));
    }

    // ═══════════════════════════════════════
    // ПОЛУЧЕНИЕ ИНФОРМАЦИИ О РЕЙСЕ
    // ═══════════════════════════════════════

    @GetMapping("/{id}")
    @Operation(
            summary = "Get flight by ID",
            description = "Get detailed information about a specific flight"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Flight found"),
            @ApiResponse(responseCode = "404", description = "Flight not found")
    })
    public ResponseEntity<FlightDetailResponse> getFlightById(
            @Parameter(description = "Flight ID")
            @PathVariable Long id
    ) {
        log.info("Get flight by id: {}", id);
        return ResponseEntity.ok(flightService.getFlightById(id));
    }

    @GetMapping("/number/{flightNumber}")
    @Operation(
            summary = "Get flight by number",
            description = "Get detailed information about a flight by its flight number"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Flight found"),
            @ApiResponse(responseCode = "404", description = "Flight not found")
    })
    public ResponseEntity<FlightDetailResponse> getFlightByNumber(
            @Parameter(description = "Flight number (e.g., SU1234)")
            @PathVariable String flightNumber
    ) {
        log.info("Get flight by number: {}", flightNumber);
        return ResponseEntity.ok(flightService.getFlightByNumber(flightNumber));
    }

    // ═══════════════════════════════════════
    // УПРАВЛЕНИЕ РЕЙСАМИ (ADMIN)
    // ═══════════════════════════════════════

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
            summary = "Create a new flight",
            description = "Create a new flight (Admin/Manager only)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Flight created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid flight data"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<FlightDetailResponse> createFlight(
            @Valid @RequestBody CreateFlightRequest request
    ) {
        log.info("Create flight request: {}", request.getFlightNumber());
        FlightDetailResponse response = flightService.createFlight(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
            summary = "Update a flight",
            description = "Update flight details (Admin/Manager only)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Flight updated successfully"),
            @ApiResponse(responseCode = "404", description = "Flight not found"),
            @ApiResponse(responseCode = "400", description = "Invalid flight data"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<FlightDetailResponse> updateFlight(
            @Parameter(description = "Flight ID")
            @PathVariable Long id,

            @Valid @RequestBody UpdateFlightRequest request
    ) {
        log.info("Update flight request: {}", id);
        return ResponseEntity.ok(flightService.updateFlight(id, request));
    }

    /**
     * ✅ ОТМЕНИТЬ РЕЙС (рекомендуется использовать вместо удаления)
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Cancel a flight",
            description = "Cancel a flight by setting its status to CANCELLED. Use this for flights with existing bookings."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Flight cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Flight not found"),
            @ApiResponse(responseCode = "400", description = "Flight is already cancelled or completed"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin only")
    })
    public ResponseEntity<FlightDetailResponse> cancelFlight(
            @Parameter(description = "Flight ID", example = "41")
            @PathVariable Long id
    ) {
        log.info("PUT /api/flights/{}/cancel - Cancelling flight", id);
        FlightDetailResponse response = flightService.cancelFlight(id);
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ УДАЛИТЬ РЕЙС (только для рейсов БЕЗ билетов)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete a flight",
            description = "Permanently delete a flight. Only works for flights with NO existing bookings. " +
                    "For flights with bookings, use the cancel endpoint instead."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Flight deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Flight not found"),
            @ApiResponse(responseCode = "409", description = "Cannot delete - flight has existing bookings"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin only")
    })
    public ResponseEntity<Void> deleteFlight(
            @Parameter(description = "Flight ID", example = "41")
            @PathVariable Long id
    ) {
        log.info("DELETE /api/flights/{} - Deleting flight", id);
        flightService.deleteFlight(id);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════
    // АЭРОПОРТЫ
    // ═══════════════════════════════════════

    @GetMapping("/airports")
    @Operation(
            summary = "Get all airports",
            description = "Get list of all active airports"
    )
    public ResponseEntity<List<AirportResponse>> getAllAirports() {
        log.info("Get all airports request");
        return ResponseEntity.ok(flightService.getAllAirports());
    }

    @GetMapping("/airports/search")
    @Operation(
            summary = "Search airports",
            description = "Search airports by IATA code, name, or city"
    )
    public ResponseEntity<List<AirportResponse>> searchAirports(
            @Parameter(description = "Search query (city, name, or IATA code)")
            @RequestParam String query
    ) {
        log.info("Search airports: {}", query);
        return ResponseEntity.ok(flightService.searchAirports(query));
    }

    @GetMapping("/airports/{iataCode}")
    @Operation(
            summary = "Get airport by code",
            description = "Get airport details by IATA code"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Airport found"),
            @ApiResponse(responseCode = "404", description = "Airport not found")
    })
    public ResponseEntity<AirportResponse> getAirportByCode(
            @Parameter(description = "Airport IATA code (e.g., DME, DXB)")
            @PathVariable String iataCode
    ) {
        log.info("Get airport by code: {}", iataCode);
        return ResponseEntity.ok(flightService.getAirportByCode(iataCode));
    }

    // ═══════════════════════════════════════
    // АВИАКОМПАНИИ
    // ═══════════════════════════════════════

    @GetMapping("/airlines")
    @Operation(
            summary = "Get all airlines",
            description = "Get list of all active airlines"
    )
    public ResponseEntity<List<AirlineResponse>> getAllAirlines() {
        log.info("Get all airlines request");
        return ResponseEntity.ok(flightService.getAllAirlines());
    }

    @GetMapping("/airlines/{iataCode}")
    @Operation(
            summary = "Get airline by code",
            description = "Get airline details by IATA code"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Airline found"),
            @ApiResponse(responseCode = "404", description = "Airline not found")
    })
    public ResponseEntity<AirlineResponse> getAirlineByCode(
            @Parameter(description = "Airline IATA code (e.g., SU, HY)")
            @PathVariable String iataCode
    ) {
        log.info("Get airline by code: {}", iataCode);
        return ResponseEntity.ok(flightService.getAirlineByCode(iataCode));
    }

    // ═══════════════════════════════════════
    // ПОПУЛЯРНЫЕ НАПРАВЛЕНИЯ
    // ═══════════════════════════════════════

    @GetMapping("/popular-destinations")
    @Operation(
            summary = "Get popular destinations",
            description = "Get most popular destination airports based on flight frequency"
    )
    public ResponseEntity<List<PopularDestinationResponse>> getPopularDestinations(
            @Parameter(description = "Number of destinations to return (1-20)")
            @RequestParam(defaultValue = "5") int limit
    ) {
        log.info("Get popular destinations, limit: {}", limit);

        // Ограничиваем максимальное значение
        int safeLimit = Math.min(limit, 20);

        return ResponseEntity.ok(flightService.getPopularDestinations(safeLimit));
    }

    @GetMapping("/flights/{flightId}/seats")
    @Operation(
            summary = "Get available seats",
            description = "Get available seats for a specific flight and cabin class"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seats information retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Flight not found")
    })
    public ResponseEntity<AvailableSeatsResponse> getAvailableSeats(
            @Parameter(description = "Flight ID", example = "36")
            @PathVariable Long flightId,

            @Parameter(description = "Cabin class", example = "BUSINESS")
            @RequestParam CabinClass cabinClass
    ) {
        log.info("Get available seats request for flight: {}, class: {}", flightId, cabinClass);

        AvailableSeatsResponse response = bookingService.getAvailableSeats(flightId, cabinClass);

        return ResponseEntity.ok(response);
    }

}
