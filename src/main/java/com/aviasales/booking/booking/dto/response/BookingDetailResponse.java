package com.aviasales.booking.booking.dto.response;

import com.aviasales.booking.booking.enums.BookingStatus;
import com.aviasales.booking.booking.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed booking information")
public class BookingDetailResponse {

    @Schema(description = "Booking ID")
    private Long id;

    @Schema(description = "Booking reference code")
    private String bookingReference;

    @Schema(description = "Booking status")
    private BookingStatus status;

    @Schema(description = "Payment status")
    private PaymentStatus paymentStatus;

    @Schema(description = "Total amount", example = "2520.00")
    private BigDecimal totalAmount;  // ✅ Исправлено с totalPrice

    @Schema(description = "Paid amount", example = "0.00")
    private BigDecimal paidAmount;

    @Schema(description = "Amount due", example = "2520.00")
    private BigDecimal amountDue;

    @Schema(description = "Booking creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Booking expiration time")
    private LocalDateTime expiresAt;

    @Schema(description = "Booking confirmation time")
    private LocalDateTime confirmedAt;

    @Schema(description = "Contact information")
    private ContactInfoResponse contactInfo;

    @Schema(description = "Special requests")
    private String specialRequests;

    @Schema(description = "List of tickets with flight details")
    private List<TicketResponse> tickets;  // ✅ Билеты с информацией о рейсах
}
