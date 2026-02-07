package com.monolit.booking.booking.dto.response;

import com.monolit.booking.booking.enums.CabinClass;
import com.monolit.booking.booking.enums.TicketStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {

    // ═══════════════════════════════════════
    // ИДЕНТИФИКАЦИЯ
    // ═══════════════════════════════════════

    private Long id;
    private String ticketNumber;
    private String eTicketNumber;

    // ═══════════════════════════════════════
    // РЕЙС (ПЛОСКАЯ СТРУКТУРА) ✅ добавил эти поля
    // ═══════════════════════════════════════

    private String flightNumber;        // ✅ SU1234
    private String originCity;          // ✅ Москва
    private String originIataCode;      // ✅ DME
    private String destinationCity;     // ✅ Дубай
    private String destinationIataCode; // ✅ DXB

    private Instant departureTimeUtc;
    private ZonedDateTime departureTime;
    private Instant arrivalTimeUtc;
    private ZonedDateTime arrivalTime;
    private Integer durationMinutes;

    // ═══════════════════════════════════════
    // ПАССАЖИР ✅ добавил это поле
    // ═══════════════════════════════════════

    private String passengerName;     // ✅ Иван Иванов

    // ═══════════════════════════════════════
    // МЕСТО И КЛАСС
    // ═══════════════════════════════════════

    private CabinClass cabinClass;
    private String seatNumber;

    // ═══════════════════════════════════════
    // ЦЕНА
    // ═══════════════════════════════════════

    private BigDecimal price;
    private BigDecimal baseFare;
    private BigDecimal taxes;
    private BigDecimal serviceFee;

    // ═══════════════════════════════════════
    // БАГАЖ
    // ═══════════════════════════════════════

    private Integer checkedBaggage;
    private Integer handLuggage;

    // ═══════════════════════════════════════
    // СТАТУС
    // ═══════════════════════════════════════

    private TicketStatus status;
    private Boolean isRefundable;
    private Boolean isChangeable;
}
