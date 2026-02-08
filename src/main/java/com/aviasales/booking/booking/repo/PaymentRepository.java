package com.aviasales.booking.booking.repo;

import com.aviasales.booking.booking.entity.Payment;
import com.aviasales.booking.booking.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingReference(String bookingReference);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByBookingId(Long bookingId);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByBookingReferenceAndStatus(String bookingReference, PaymentStatus status);

    Optional<Payment> findFirstByBookingIdAndStatus(Long bookingId, PaymentStatus status);

    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
