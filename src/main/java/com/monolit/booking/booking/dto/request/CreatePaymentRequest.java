package com.monolit.booking.booking.dto.request;

import com.monolit.booking.booking.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotBlank(message = "Booking reference is required")
    @Size(min = 6, max = 6, message = "Booking reference must be 6 characters")
    private String bookingReference;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String cardNumber;

    private String cardHolderName;

    private String expiryDate;

    private String cvv;
}
