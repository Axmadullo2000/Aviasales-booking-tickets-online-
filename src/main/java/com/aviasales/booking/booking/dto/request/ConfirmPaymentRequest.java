package com.aviasales.booking.booking.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPaymentRequest {

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    private String verificationCode;

    private String cvv;
}
