package com.aviasales.booking.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Round-trip discount calculation")
public class RoundTripDiscountResponse {

    // ═══════════════════════════════════════
    // ИНФОРМАЦИЯ О РЕЙСАХ
    // ═══════════════════════════════════════

    @Schema(description = "Outbound flight summary")
    private FlightSummary outboundFlight;

    @Schema(description = "Return flight summary")
    private FlightSummary returnFlight;

    // ═══════════════════════════════════════
    // РАСЧЕТ ЦЕН
    // ═══════════════════════════════════════

    @Schema(description = "Outbound flight price (before discount)", example = "450.00")
    private BigDecimal outboundPrice;

    @Schema(description = "Return flight price (before discount)", example = "420.00")
    private BigDecimal returnPrice;

    @Schema(description = "Total price before discount", example = "870.00")
    private BigDecimal totalPriceBeforeDiscount;

    @Schema(description = "Discount percentage", example = "5.0")
    private BigDecimal discountPercent;

    @Schema(description = "Discount amount", example = "43.50")
    private BigDecimal discountAmount;

    @Schema(description = "Total price after discount", example = "826.50")
    private BigDecimal totalPriceAfterDiscount;

    @Schema(description = "Total savings", example = "43.50")
    private BigDecimal totalSavings;

    // ═══════════════════════════════════════
    // ДОПОЛНИТЕЛЬНАЯ ИНФОРМАЦИЯ
    // ═══════════════════════════════════════

    @Schema(description = "Discount reason/message",
            example = "5% discount applied for round-trip booking")
    private String discountMessage;

    @Schema(description = "Is this a good deal?", example = "true")
    private Boolean isGoodDeal;

    @Schema(description = "Recommendation",
            example = "Great deal! Save $43.50 by booking round-trip")
    private String recommendation;

    // ═══════════════════════════════════════
    // ВЛОЖЕННЫЙ КЛАСС - КРАТКАЯ ИНФОРМАЦИЯ О РЕЙСЕ
    // ═══════════════════════════════════════

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Flight summary information")
    public static class FlightSummary {

        @Schema(description = "Flight ID", example = "36")
        private Long flightId;

        @Schema(description = "Flight number", example = "HY404")
        private String flightNumber;

        @Schema(description = "Route", example = "DME → TAS")
        private String route;

        @Schema(description = "Departure time", example = "2026-03-18T15:00:00Z")
        private String departureTime;

        @Schema(description = "Arrival time", example = "2026-03-18T19:00:00Z")
        private String arrivalTime;

        @Schema(description = "Duration", example = "4h 0m")
        private String duration;

        @Schema(description = "Airline", example = "Uzbekistan Airways")
        private String airline;

        @Schema(description = "Available seats", example = "142")
        private Integer availableSeats;
    }
}
