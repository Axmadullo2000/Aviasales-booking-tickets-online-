package com.aviasales.booking.booking.controller;

import com.aviasales.booking.booking.dto.response.CalendarPriceResponse;
import com.aviasales.booking.booking.dto.response.DynamicPriceResponse;
import com.aviasales.booking.booking.entity.Flight;
import com.aviasales.booking.booking.enums.CabinClass;
import com.aviasales.booking.booking.repo.FlightRepository;
import com.aviasales.booking.booking.service.interfaces.CalendarService;
import com.aviasales.booking.booking.service.interfaces.FlightService;
import com.aviasales.booking.booking.service.interfaces.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * API для работы с динамическими ценами и календарём
 */
@Slf4j
@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
@Tag(name = "Pricing", description = "Dynamic pricing and calendar APIs")
public class PricingController {

    private final PricingService pricingService;
    private final CalendarService calendarService;
    private final FlightService flightService;

    /**
     * Получить динамическую цену для конкретного рейса
     */
    @GetMapping("/flights/{flightId}")
    @Operation(summary = "Get dynamic price for specific flight",
            description = "Returns detailed price breakdown with all applied multipliers")
    public ResponseEntity<DynamicPriceResponse> getFlightPrice(
            @Parameter(description = "Flight ID")
            @PathVariable Long flightId,

            @Parameter(description = "Cabin class (ECONOMY, BUSINESS, FIRST_CLASS)")
            @RequestParam(defaultValue = "ECONOMY") CabinClass cabinClass,

            @Parameter(description = "Booking date (defaults to today)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate
    ) {
        log.info("GET /api/pricing/flights/{} - cabinClass={}, bookingDate={}",
                flightId, cabinClass, bookingDate);

        Flight flight = flightService.findById(flightId);

        LocalDate booking = bookingDate != null ? bookingDate : LocalDate.now();

        DynamicPriceResponse response = pricingService.calculateDynamicPrice(
                flight, cabinClass, booking
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Получить календарь цен на месяц
     */
    @GetMapping("/calendar")
    @Operation(summary = "Get monthly price calendar",
            description = "Returns prices for each day of the month for a specific route")
    public ResponseEntity<CalendarPriceResponse> getMonthlyCalendar(
            @Parameter(description = "Origin airport IATA code", example = "TAS")
            @RequestParam String from,

            @Parameter(description = "Destination airport IATA code", example = "DME")
            @RequestParam String to,

            @Parameter(description = "Month in YYYY-MM format", example = "2026-03")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,

            @Parameter(description = "Cabin class")
            @RequestParam(defaultValue = "ECONOMY") CabinClass cabinClass
    ) {
        log.info("GET /api/pricing/calendar - route={}->{}, month={}, class={}",
                from, to, month, cabinClass);

        CalendarPriceResponse response = calendarService.getMonthlyPrices(
                from.toUpperCase(),
                to.toUpperCase(),
                month,
                cabinClass
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Получить лучшие цены на ближайшие 3 месяца
     */
    @GetMapping("/best-prices")
    @Operation(summary = "Get best prices for next 3 months",
            description = "Returns the cheapest day in each of the next 3 months")
    public ResponseEntity<?> getBestPricesNextMonths(
            @Parameter(description = "Origin airport IATA code")
            @RequestParam String from,

            @Parameter(description = "Destination airport IATA code")
            @RequestParam String to,

            @Parameter(description = "Cabin class")
            @RequestParam(defaultValue = "ECONOMY") CabinClass cabinClass
    ) {
        log.info("GET /api/pricing/best-prices - route={}->{}, class={}", from, to, cabinClass);

        // Получаем текущий месяц + следующие 2
        YearMonth currentMonth = YearMonth.now();

        CalendarPriceResponse month1 = calendarService.getMonthlyPrices(
                from.toUpperCase(), to.toUpperCase(), currentMonth, cabinClass
        );

        CalendarPriceResponse month2 = calendarService.getMonthlyPrices(
                from.toUpperCase(), to.toUpperCase(), currentMonth.plusMonths(1), cabinClass
        );

        CalendarPriceResponse month3 = calendarService.getMonthlyPrices(
                from.toUpperCase(), to.toUpperCase(), currentMonth.plusMonths(2), cabinClass
        );

        return ResponseEntity.ok(java.util.Map.of(
                "currentMonth", month1,
                "nextMonth", month2,
                "twoMonthsAhead", month3
        ));
    }
}
