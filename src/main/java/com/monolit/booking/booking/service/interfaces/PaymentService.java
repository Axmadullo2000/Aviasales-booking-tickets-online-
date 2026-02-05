package com.monolit.booking.booking.service.interfaces;

import com.monolit.booking.booking.dto.request.*;
import com.monolit.booking.booking.dto.response.*;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest request, Long userId);

    PaymentResponse confirmPayment(ConfirmPaymentRequest request, Long userId);

    PaymentStatusResponse getPaymentStatus(String transactionId);

    PaymentResponse getPaymentByBookingReference(String bookingReference);

    PaymentResponse refundPayment(Long paymentId, Long userId);
}
