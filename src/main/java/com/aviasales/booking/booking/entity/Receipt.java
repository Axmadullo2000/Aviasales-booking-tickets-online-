package com.aviasales.booking.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "receipts")
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String receiptNumber;

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String bookingReference;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal serviceFee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(nullable = false)
    private String paymentMethod;

    @Column(length = 4)
    private String cardLastFour;

    @Column(columnDefinition = "TEXT")
    private String passengerName;

    @Column(columnDefinition = "TEXT")
    private String flightDetails;

    @CreationTimestamp
    private Instant createdAt;

    private Instant paymentDate;
}
