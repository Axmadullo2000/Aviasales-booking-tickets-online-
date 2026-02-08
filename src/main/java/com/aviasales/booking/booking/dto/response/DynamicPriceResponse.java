package com.aviasales.booking.booking.dto.response;

import com.aviasales.booking.booking.enums.DemandLevel;
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
@Schema(description = "Dynamic price calculation result")
public class DynamicPriceResponse {

    @Schema(description = "Base price before multipliers", example = "300.00")
    private BigDecimal basePrice;

    @Schema(description = "Final price after multipliers", example = "450.00")
    private BigDecimal finalPrice;

    @Schema(description = "Taxes and fees", example = "67.50")
    private BigDecimal taxes;

    @Schema(description = "Total price including taxes", example = "517.50")
    private BigDecimal totalPrice;

    @Schema(description = "Occupancy percentage", example = "75")
    private Integer occupancyPercent;

    @Schema(description = "Days until departure", example = "14")
    private Long daysUntilDeparture;

    @Schema(description = "Occupancy multiplier", example = "1.3")
    private BigDecimal occupancyMultiplier;

    @Schema(description = "Time multiplier", example = "1.15")
    private BigDecimal timeMultiplier;

    @Schema(description = "Day of week multiplier", example = "1.2")
    private BigDecimal dayOfWeekMultiplier;

    @Schema(description = "Demand level", example = "HIGH")
    private DemandLevel demandLevel;

    @Schema(description = "Price recommendation message", example = "Good price! Book now before it increases")
    private String recommendation;
}
