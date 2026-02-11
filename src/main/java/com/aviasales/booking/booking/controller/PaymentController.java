package com.aviasales.booking.booking.controller;

import com.aviasales.booking.booking.dto.request.ConfirmPaymentRequest;
import com.aviasales.booking.booking.dto.request.CreatePaymentRequest;
import com.aviasales.booking.booking.dto.response.PaymentResponse;
import com.aviasales.booking.booking.dto.response.PaymentStatusResponse;
import com.aviasales.booking.booking.dto.response.ReceiptResponse;
import com.aviasales.booking.booking.projection.UsersProjection;
import com.aviasales.booking.booking.service.interfaces.PaymentService;
import com.aviasales.booking.booking.service.interfaces.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing endpoints")
public class PaymentController {

    private final PaymentService paymentService;
    private final ReceiptService receiptService;

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

    @GetMapping("/receipt/{bookingReference}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Get receipt by booking", description = "Get receipt details for a completed payment")
    public ResponseEntity<ReceiptResponse> getReceiptByBookingReference(
            @Parameter(description = "Booking reference")
            @PathVariable String bookingReference,
            @AuthenticationPrincipal UsersProjection user
    ) {
        return ResponseEntity.ok(receiptService.getReceiptByBookingReference(bookingReference, user.getId()));
    }

    @GetMapping("/receipt/{bookingReference}/download")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Download receipt PDF", description = "Download the payment receipt as PDF")
    public ResponseEntity<byte[]> downloadReceiptPdf(
            @Parameter(description = "Booking reference")
            @PathVariable String bookingReference,
            @AuthenticationPrincipal UsersProjection user
    ) {
        byte[] pdfContent = receiptService.generateReceiptPdf(bookingReference, user.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "receipt-" + bookingReference + ".pdf");
        headers.setContentLength(pdfContent.length);

        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }
}
