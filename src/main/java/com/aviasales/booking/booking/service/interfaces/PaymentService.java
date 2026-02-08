package com.aviasales.booking.booking.service.interfaces;

import com.aviasales.booking.booking.dto.request.ConfirmPaymentRequest;
import com.aviasales.booking.booking.dto.request.CreatePaymentRequest;
import com.aviasales.booking.booking.dto.response.PaymentResponse;
import com.aviasales.booking.booking.dto.response.PaymentStatusResponse;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest request, Long userId);

    PaymentResponse confirmPayment(ConfirmPaymentRequest request, Long userId);

    PaymentStatusResponse getPaymentStatus(String transactionId);

    PaymentResponse getPaymentByBookingReference(String bookingReference);

    PaymentResponse refundPayment(Long paymentId, Long userId);
}
