package com.monolit.booking.booking.controller;

import com.monolit.booking.booking.dto.request.ConfirmPaymentRequest;
import com.monolit.booking.booking.dto.request.CreatePaymentRequest;
import com.monolit.booking.booking.dto.response.PaymentResponse;
import com.monolit.booking.booking.dto.response.PaymentStatusResponse;
import com.monolit.booking.booking.entity.Users;
import com.monolit.booking.booking.projection.UsersProjection;
import com.monolit.booking.booking.service.interfaces.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Create a payment", description = "Process payment for a booking")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal UsersProjection user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(request, user.getId()));
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Confirm a payment", description = "Confirm a payment with verification code (3DS)")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @Valid @RequestBody ConfirmPaymentRequest request,
            @AuthenticationPrincipal UsersProjection user
    ) {
        return ResponseEntity.ok(paymentService.confirmPayment(request, user.getId()));
    }

    @GetMapping("/status/{transactionId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Get payment status", description = "Get the status of a payment by transaction ID")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @Parameter(description = "Transaction ID")
            @PathVariable String transactionId
    ) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(transactionId));
    }

    @GetMapping("/booking/{bookingReference}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Get payment by booking", description = "Get payment details for a booking")
    public ResponseEntity<PaymentResponse> getPaymentByBookingReference(
            @Parameter(description = "Booking reference")
            @PathVariable String bookingReference
    ) {
        return ResponseEntity.ok(paymentService.getPaymentByBookingReference(bookingReference));
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refund a payment", description = "Process a refund for a completed payment (Admin only)")
    public ResponseEntity<PaymentResponse> refundPayment(
            @Parameter(description = "Payment ID")
            @PathVariable Long paymentId,
            @AuthenticationPrincipal UsersProjection user
    ) {
        return ResponseEntity.ok(paymentService.refundPayment(paymentId, user.getId()));
    }
}
