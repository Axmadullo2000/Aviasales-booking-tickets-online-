package com.monolit.booking.booking.dto.response;

import com.monolit.booking.booking.enums.FlightStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightResponse {

    private Long id;
    private String flightNumber;

    private AirportResponse origin;
    private AirportResponse destination;
    private AirlineResponse airline;

    // ═══════════════════════════════════════
    // ВРЕМЯ (ДВА ВАРИАНТА!)
    // ═══════════════════════════════════════

    /**
     * Время вылета в UTC (для внутренних расчётов)
     * Формат JSON: "2026-03-15T07:30:00Z"
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant departureTimeUtc;

    /**
     * Время вылета в локальном времени аэропорта вылета (для пользователя)
     * Формат JSON: "2026-03-15T10:30:00+03:00[Europe/Moscow]"
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private ZonedDateTime departureTime;

    /**
     * Время прилёта в UTC (для внутренних расчётов)
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant arrivalTimeUtc;

    /**
     * Время прилёта в локальном времени аэропорта прилёта (для пользователя)
     * Формат JSON: "2026-03-15T16:45:00+04:00[Asia/Dubai]"
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private ZonedDateTime arrivalTime;

    /**
     * Длительность полёта в минутах
     */
    private Integer durationMinutes;

    // ═══════════════════════════════════════
    // ЦЕНЫ
    // ═══════════════════════════════════════

    private BigDecimal economyPrice;
    private BigDecimal businessPrice;
    private BigDecimal firstClassPrice;

    // ═══════════════════════════════════════
    // ДОСТУПНОСТЬ
    // ═══════════════════════════════════════

    private Integer availableSeats;
    private FlightStatus status;

    // ═══════════════════════════════════════
    // ДОПОЛНИТЕЛЬНО
    // ═══════════════════════════════════════

    private String aircraftType;
    private Integer stops;
}
