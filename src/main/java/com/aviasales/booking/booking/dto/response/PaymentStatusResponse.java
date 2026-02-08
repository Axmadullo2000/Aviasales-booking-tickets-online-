package com.aviasales.booking.booking.dto.response;

import com.aviasales.booking.booking.enums.PaymentStatus;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {

    private PaymentStatus status;
    private String transactionId;
    private Instant processedAt;
    private String failureReason;
}
