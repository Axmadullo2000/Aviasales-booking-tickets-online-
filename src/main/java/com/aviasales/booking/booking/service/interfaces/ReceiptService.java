package com.aviasales.booking.booking.service.interfaces;

import com.aviasales.booking.booking.dto.response.ReceiptResponse;
import com.aviasales.booking.booking.entity.Booking;
import com.aviasales.booking.booking.entity.Payment;

public interface ReceiptService {

    ReceiptResponse createReceipt(Payment payment, Booking booking);

    ReceiptResponse getReceiptByBookingReference(String bookingReference, Long userId);

    ReceiptResponse getReceiptByTransactionId(String transactionId);

    byte[] generateReceiptPdf(String bookingReference, Long userId);
}
