package com.monolit.booking.booking.entity;

import com.monolit.booking.booking.embedded.ContactInfo;  // ✅ исправил путь
import com.monolit.booking.booking.enums.BookingStatus;
import com.monolit.booking.booking.enums.PaymentMethod;
import com.monolit.booking.booking.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;  // ✅ вместо @CreationTimestamp
import org.springframework.data.annotation.LastModifiedDate;  // ✅ добавил
import org.springframework.data.jpa.domain.support.AuditingEntityListener;  // ✅ добавил

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;  // ✅ вместо Instant
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_reference", columnList = "bookingReference"),
        @Index(name = "idx_user_status", columnList = "user_id, status")
})
@EntityListeners(AuditingEntityListener.class)  // ✅ для @CreatedDate
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ═══════════════════════════════════════
    // ИДЕНТИФИКАЦИЯ
    // ═══════════════════════════════════════

    @Column(unique = true, nullable = false, length = 10)
    private String bookingReference;  // ABC123XY

    // ═══════════════════════════════════════
    // СВЯЗИ
    // ═══════════════════════════════════════

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)  // ✅ добавил nullable = false
    private Users user;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default  // ✅ добавил для Builder
    private List<Ticket> tickets = new ArrayList<>();

    // ═══════════════════════════════════════
    // КОНТАКТНАЯ ИНФОРМАЦИЯ
    // ═══════════════════════════════════════

    @Embedded
    private ContactInfo contactInfo;

    // ═══════════════════════════════════════
    // ФИНАНСЫ
    // ═══════════════════════════════════════

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;  // ✅ переименовал с totalPrice

    @Column(precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;  // ✅ добавил default

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    // ═══════════════════════════════════════
    // СТАТУС БРОНИРОВАНИЯ
    // ═══════════════════════════════════════

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    // ═══════════════════════════════════════
    // ВРЕМЕННЫЕ РАМКИ
    // ═══════════════════════════════════════

    @CreatedDate  // ✅ Spring Data JPA аннотация
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate  // ✅ добавил для автообновления
    private LocalDateTime updatedAt;

    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;  // до какого времени держим без оплаты

    // ═══════════════════════════════════════
    // ДОПОЛНИТЕЛЬНАЯ ИНФОРМАЦИЯ
    // ═══════════════════════════════════════

    @Column(columnDefinition = "TEXT")
    private String specialRequests;

    private String cancellationReason;

    @Column(precision = 10, scale = 2)
    private BigDecimal refundAmount;

    // ═══════════════════════════════════════
    // LIFECYCLE CALLBACKS
    // ═══════════════════════════════════════

    @PrePersist
    public void generateBookingReference() {
        if (bookingReference == null) {
            // Генерируем 6-символьный код (ABC123)
            this.bookingReference = generateRandomReference();
        }
        if (expiresAt == null) {
            // 15 минут на оплату
            this.expiresAt = LocalDateTime.now().plusMinutes(15);
        }
    }

    // ═══════════════════════════════════════
    // БИЗНЕС-ЛОГИКА
    // ═══════════════════════════════════════

    public boolean isExpired() {
        return status == BookingStatus.PENDING
                && LocalDateTime.now().isAfter(expiresAt);
    }

    public void confirm() {
        if (this.status != BookingStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot confirm booking in status: " + status
            );
        }
        this.status = BookingStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.paymentStatus = PaymentStatus.PAID;
        this.paidAmount = this.totalAmount;
    }

    public void cancel(String reason) {
        if (this.status == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking already cancelled");
        }

        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;

        // ✅ Освобождаем места (безопасно с lazy loading)
        if (tickets != null && !tickets.isEmpty()) {
            tickets.forEach(ticket -> {
                if (ticket.getFlight() != null) {
                    ticket.getFlight().releaseSeats(1);
                }
            });
        }
    }

    public BigDecimal calculateRefund() {
        // Логика расчёта возврата зависит от:
        // - времени до вылета
        // - правил тарифа (refundable/non-refundable)
        // - политики авиакомпании

        if (status != BookingStatus.CONFIRMED) {
            return BigDecimal.ZERO;
        }

        // Упрощённая логика (можно вынести в отдельный сервис)
        Instant firstFlightDeparture = tickets.stream()
                .map(ticket -> ticket.getFlight().getDepartureTime())
                .min(Comparator.naturalOrder())
                .orElse(Instant.now());

        long hoursUntilDeparture = Duration.between(
                LocalDateTime.now(),
                firstFlightDeparture
        ).toHours();

        // > 24 часа = 90% возврат
        if (hoursUntilDeparture > 24) {
            return totalAmount.multiply(new BigDecimal("0.90"));
        }
        // 2-24 часа = 50% возврат
        else if (hoursUntilDeparture > 2) {
            return totalAmount.multiply(new BigDecimal("0.50"));
        }
        // < 2 часа = без возврата
        else {
            return BigDecimal.ZERO;
        }
    }

    // ═══════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════

    private String generateRandomReference() {
        // Генерируем 6-символьный код: буквы + цифры (ABC123)
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(6);
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }

    public int getTotalPassengers() {
        return tickets != null ? tickets.size() : 0;
    }

    public boolean isPaid() {
        return paymentStatus == PaymentStatus.PAID;
    }

    public boolean canBeCancelled() {
        return status == BookingStatus.CONFIRMED
                || status == BookingStatus.PENDING;
    }
}