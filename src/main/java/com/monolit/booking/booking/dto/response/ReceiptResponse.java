package com.monolit.booking.booking.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ReceiptResponse {

    private Long id;
    private String receiptNumber;
    private String transactionId;
    private String bookingReference;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal serviceFee;
    private BigDecimal totalAmount;
    private String currency;
    private String paymentMethod;
    private String cardLastFour;
    private String passengerName;
    private String flightDetails;
    private OffsetDateTime createdAt;
    private OffsetDateTime paymentDate;
}
