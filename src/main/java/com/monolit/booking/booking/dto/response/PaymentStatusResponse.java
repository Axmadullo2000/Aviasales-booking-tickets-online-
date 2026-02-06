package com.monolit.booking.booking.dto.response;

import com.monolit.booking.booking.enums.PaymentStatus;
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
