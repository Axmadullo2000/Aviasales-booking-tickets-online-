package com.monolit.booking.booking.service.interfaces;

import com.monolit.booking.booking.dto.response.ReceiptResponse;
import com.monolit.booking.booking.entity.Booking;
import com.monolit.booking.booking.entity.Payment;

public interface ReceiptService {

    ReceiptResponse createReceipt(Payment payment, Booking booking);

    ReceiptResponse getReceiptByBookingReference(String bookingReference, Long userId);

    ReceiptResponse getReceiptByTransactionId(String transactionId);

    byte[] generateReceiptPdf(String bookingReference, Long userId);
}
