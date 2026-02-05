package com.monolit.booking.booking.repo;

import com.monolit.booking.booking.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    Optional<Receipt> findByReceiptNumber(String receiptNumber);

    Optional<Receipt> findByPaymentId(Long paymentId);

    Optional<Receipt> findByTransactionId(String transactionId);

    Optional<Receipt> findByBookingReference(String bookingReference);
}
