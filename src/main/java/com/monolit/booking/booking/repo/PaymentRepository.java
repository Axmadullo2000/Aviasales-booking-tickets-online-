package com.monolit.booking.booking.repo;

import com.monolit.booking.booking.entity.Payment;
import com.monolit.booking.booking.enums.PaymentStatus;
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
}
