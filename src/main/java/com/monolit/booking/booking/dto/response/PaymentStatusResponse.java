package com.monolit.booking.booking.dto.response;

import com.monolit.booking.booking.enums.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {

    private PaymentStatus status;
    private String transactionId;
    private OffsetDateTime processedAt;
    private String failureReason;
}
