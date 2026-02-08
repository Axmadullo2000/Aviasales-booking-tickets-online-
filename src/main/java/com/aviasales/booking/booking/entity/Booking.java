package com.aviasales.booking.booking.entity;

import com.aviasales.booking.booking.embedded.ContactInfo;
import com.aviasales.booking.booking.enums.BookingStatus;
import com.aviasales.booking.booking.enums.PaymentMethod;
import com.aviasales.booking.booking.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
@EntityListeners(AuditingEntityListener.class)
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
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
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
    private BigDecimal totalAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal refundAmount;  // ✅ сумма возврата

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

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

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
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

    @Column(length = 500)
    private String cancellationReason;

    // ═══════════════════════════════════════
    // LIFECYCLE CALLBACKS
    // ═══════════════════════════════════════

    @PrePersist
    public void generateBookingReference() {
        if (bookingReference == null) {
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

    /**
     * Отметить бронирование как отменённое
     * ВАЖНО: Освобождение мест и отмена билетов должны делаться в сервисе!
     * Этот метод только обновляет статус бронирования
     *
     * @param reason причина отмены
     */
    public void markAsCancelled(String reason) {
        if (this.status == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking already cancelled");
        }

        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;

        // Обновляем платежный статус
        if (this.paymentStatus == PaymentStatus.PAID) {
            this.paymentStatus = PaymentStatus.REFUND_PENDING;
        }
    }

    /**
     * Рассчитать общую сумму возврата по всем билетам
     * Просто суммирует refundAmount из каждого билета
     *
     * @return общая сумма возврата
     */
    public BigDecimal calculateTotalRefund() {
        if (tickets == null || tickets.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return tickets.stream()
                .map(ticket -> ticket.getRefundAmount() != null
                        ? ticket.getRefundAmount()
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Получить минимальное время до вылета среди всех рейсов
     */
    public long getMinHoursUntilDeparture() {
        if (tickets == null || tickets.isEmpty()) {
            return 0;
        }

        return tickets.stream()
                .filter(ticket -> ticket.getFlight() != null)
                .mapToLong(ticket -> ticket.getFlight().getHoursUntilDeparture())
                .min()
                .orElse(0);
    }

    /**
     * Проверить все ли билеты возвратные
     */
    public boolean hasRefundableTickets() {
        if (tickets == null || tickets.isEmpty()) {
            return false;
        }

        return tickets.stream()
                .anyMatch(ticket -> Boolean.TRUE.equals(ticket.getIsRefundable()));
    }

    /**
     * Получить процент потенциального возврата
     */
    public int getRefundPercentage() {
        long hours = getMinHoursUntilDeparture();

        if (!hasRefundableTickets()) {
            return 0;
        }

        if (hours < 24) return 50;
        if (hours < 48) return 70;
        if (hours < 168) return 80;
        return 100;
    }

    // ═══════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════

    private String generateRandomReference() {
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

    /**
     * Проверить истекло ли время для оплаты
     */
    public boolean isPaymentExpired() {
        return status == BookingStatus.PENDING
                && expiresAt != null
                && LocalDateTime.now().isAfter(expiresAt);
    }
}
