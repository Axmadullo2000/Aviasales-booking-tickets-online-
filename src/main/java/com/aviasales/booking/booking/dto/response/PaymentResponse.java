package com.aviasales.booking.booking.dto.response;

import com.aviasales.booking.booking.enums.PaymentStatus;
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
@Schema(description = "Payment response")
public class PaymentResponse {

    @Schema(description = "Payment ID")
    private Long id;

    @Schema(description = "Transaction ID")
    private String transactionId;

    @Schema(description = "Booking reference")
    private String bookingReference;

    @Schema(description = "Payment amount", example = "2520.00")
    private BigDecimal amount;

    @Schema(description = "Change amount (if overpaid)", example = "0.00")
    private BigDecimal changeAmount;  // ✅ НОВОЕ ПОЛЕ

    @Schema(description = "Currency", example = "USD")
    private String currency;

    @Schema(description = "Payment method")
    private String paymentMethod;

    @Schema(description = "Payment status")
    private PaymentStatus status;

    @Schema(description = "Last 4 digits of card")
    private String cardLastFour;

    @Schema(description = "Payment creation time")
    private Instant createdAt;

    @Schema(description = "Payment processing time")
    private Instant processedAt;

    @Schema(description = "Failure reason (if failed)")
    private String failureReason;

    @Schema(description = "Response message")
    private String message;  // ✅ НОВОЕ ПОЛЕ
}
