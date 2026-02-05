package com.monolit.booking.booking.dto.response;

import com.monolit.booking.booking.enums.PaymentMethod;
import com.monolit.booking.booking.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private String bookingReference;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String transactionId;
    private String cardLastFour;
    private OffsetDateTime createdAt;
}
