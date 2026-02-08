package com.aviasales.booking.booking.dto.response;

import com.aviasales.booking.booking.enums.BookingStatus;
import com.aviasales.booking.booking.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Booking response")
public class BookingResponse {

    @Schema(description = "Booking ID", example = "11")
    private Long id;

    @Schema(description = "Booking reference code", example = "QEDH31")
    private String bookingReference;

    @Schema(description = "Booking status", example = "PENDING")
    private BookingStatus status;

    @Schema(description = "Payment status", example = "PENDING")
    private PaymentStatus paymentStatus;

    @Schema(description = "Total amount to pay", example = "2520.00")
    private BigDecimal totalAmount;  // ✅ НЕ totalPrice!

    @Schema(description = "Amount already paid", example = "0.00")
    private BigDecimal paidAmount;

    @Schema(description = "Amount remaining to pay", example = "2520.00")
    private BigDecimal amountDue;

    @Schema(description = "Number of passengers", example = "9")
    private Integer totalPassengers;

    @Schema(description = "Booking creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Booking expiration time")
    private LocalDateTime expiresAt;

    @Schema(description = "Payment instructions")
    private String paymentInstructions;
}
