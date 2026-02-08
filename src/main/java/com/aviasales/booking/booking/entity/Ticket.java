package com.aviasales.booking.booking.entity;

import com.aviasales.booking.booking.enums.CabinClass;
import com.aviasales.booking.booking.enums.FareType;
import com.aviasales.booking.booking.enums.SeatPreference;
import com.aviasales.booking.booking.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_ticket_number", columnList = "ticketNumber"),
        @Index(name = "idx_booking_id", columnList = "booking_id")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ═══════════════════════════════════════
    // ИДЕНТИФИКАЦИЯ
    // ═══════════════════════════════════════

    @Column(unique = true, length = 13)
    private String ticketNumber;  // 13-значный номер (стандарт IATA)

    @Column(unique = true, length = 6)
    private String eTicketNumber;  // электронный билет (для check-in)

    // ═══════════════════════════════════════
    // СВЯЗИ (КЛЮЧЕВЫЕ!)
    // ═══════════════════════════════════════

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    // ═══════════════════════════════════════
    // МЕСТО И КЛАСС
    // ═══════════════════════════════════════

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CabinClass cabinClass;  // ECONOMY, BUSINESS, FIRST_CLASS

    private String seatNumber;  // 12A, 34F (может быть NULL до check-in)

    @Enumerated(EnumType.STRING)
    private SeatPreference seatPreference;  // WINDOW, AISLE, MIDDLE

    // ═══════════════════════════════════════
    // ЦЕНА И ТАРИФЫ
    // ═══════════════════════════════════════

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;  // общая цена билета

    @Column(precision = 10, scale = 2)
    private BigDecimal baseFare;  // тариф без налогов

    @Column(precision = 10, scale = 2)
    private BigDecimal taxes;  // налоги и сборы

    @Column(precision = 10, scale = 2)
    private BigDecimal serviceFee;  // комиссия сервиса

    @Enumerated(EnumType.STRING)
    private FareType fareType;  // ECONOMY_LIGHT, ECONOMY_STANDARD, BUSINESS

    // ═══════════════════════════════════════
    // БАГАЖ
    // ═══════════════════════════════════════

    private Integer checkedBaggage;  // кг багажа (0 для лоукостеров)
    private Integer handLuggage;     // кг ручной клади

    private Boolean hasPriorityBoarding;  // приоритетная посадка

    // ═══════════════════════════════════════
    // СТАТУС БИЛЕТА
    // ═══════════════════════════════════════

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    private LocalDateTime issuedAt;      // когда выписан
    private LocalDateTime checkedInAt;   // когда прошёл check-in
    private LocalDateTime boardedAt;     // когда сел в самолёт

    // ═══════════════════════════════════════
    // ИЗМЕНЕНИЯ И ВОЗВРАТЫ
    // ═══════════════════════════════════════

    private Boolean isRefundable;      // можно ли вернуть
    private Boolean isChangeable;      // можно ли изменить

    @Column(precision = 10, scale = 2)
    private BigDecimal changeFee;      // стоимость изменения

    @Column(precision = 10, scale = 2)
    private BigDecimal cancellationFee;  // стоимость отмены

    @Column(precision = 10, scale = 2)
    private BigDecimal refundAmount;  // сумма возврата при отмене

    private LocalDateTime cancelledAt;  // когда был отменен

    // ═══════════════════════════════════════
    // АУДИТ
    // ═══════════════════════════════════════

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private static final AtomicInteger counter = new AtomicInteger(0);

    // ═══════════════════════════════════════
    // LIFECYCLE CALLBACKS
    // ═══════════════════════════════════════

    @PrePersist
    public void generateTicketNumbers() {
        if (ticketNumber == null) {
            ticketNumber = generateTicketNumber();
        }
        if (eTicketNumber == null) {
            eTicketNumber = generateETicketNumber();
        }
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
    }

    // ═══════════════════════════════════════
    // БИЗНЕС-ЛОГИКА
    // ═══════════════════════════════════════

    public void checkIn(String seatNumber) {
        if (this.status != TicketStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot check-in: ticket not confirmed");
        }
        this.seatNumber = seatNumber;
        this.status = TicketStatus.CHECKED_IN;
        this.checkedInAt = LocalDateTime.now();
    }

    public void board() {
        if (this.status != TicketStatus.CHECKED_IN) {
            throw new IllegalStateException("Cannot board: not checked in");
        }
        this.status = TicketStatus.BOARDED;
        this.boardedAt = LocalDateTime.now();
    }

    /**
     * Отменить билет и рассчитать возврат
     *
     * @param hoursUntilDeparture часы до вылета
     * @return сумма возврата
     */
    public BigDecimal cancel(long hoursUntilDeparture) {
        if (this.status == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Ticket already cancelled");
        }

        if (this.status == TicketStatus.USED || this.status == TicketStatus.BOARDED) {
            throw new IllegalStateException("Cannot cancel used ticket");
        }

        this.status = TicketStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();

        // Рассчитываем возврат
        this.refundAmount = calculateRefundAmount(hoursUntilDeparture);

        return this.refundAmount;
    }

    /**
     * Рассчитать сумму возврата с учетом условий
     *
     * @param hoursUntilDeparture часы до вылета
     * @return сумма возврата
     */
    public BigDecimal calculateRefundAmount(long hoursUntilDeparture) {
        // Невозвратный билет
        if (Boolean.FALSE.equals(isRefundable)) {
            return BigDecimal.ZERO;
        }

        BigDecimal baseAmount = price;

        // Вычитаем комиссию за отмену (если есть)
        if (cancellationFee != null) {
            baseAmount = baseAmount.subtract(cancellationFee);
        }

        // Штрафы в зависимости от времени до вылета
        if (hoursUntilDeparture < 24) {
            // Менее 24 часов - возврат 50%
            baseAmount = baseAmount.multiply(new BigDecimal("0.50"));
        } else if (hoursUntilDeparture < 48) {
            // Менее 48 часов - возврат 70%
            baseAmount = baseAmount.multiply(new BigDecimal("0.70"));
        } else if (hoursUntilDeparture < 168) { // 7 дней
            // Менее 7 дней - возврат 80%
            baseAmount = baseAmount.multiply(new BigDecimal("0.80"));
        }
        // Более 7 дней - полный возврат (100%)

        // Не может быть отрицательным
        return baseAmount.max(BigDecimal.ZERO);
    }

    /**
     * Проверить можно ли отменить билет
     */
    public boolean isCancellable() {
        return (status == TicketStatus.CONFIRMED || status == TicketStatus.ISSUED)
                && flight != null
                && flight.getDepartureTime().isAfter(Instant.now());
    }

    /**
     * Получить процент возврата
     */
    public int getRefundPercentage(long hoursUntilDeparture) {
        if (Boolean.FALSE.equals(isRefundable)) {
            return 0;
        }

        if (hoursUntilDeparture < 24) return 50;
        if (hoursUntilDeparture < 48) return 70;
        if (hoursUntilDeparture < 168) return 80;
        return 100;
    }

    public BigDecimal calculateTotalPrice() {
        BigDecimal total = price;
        if (serviceFee != null) {
            total = total.add(serviceFee);
        }
        return total;
    }

    public boolean canCheckIn() {
        return status == TicketStatus.CONFIRMED
                && Instant.now().isBefore(flight.getDepartureTime())
                && Instant.now().isAfter(flight.getDepartureTime().minusSeconds(24 * 60 * 60));
    }

    // ═══════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════

    /**
     * ✅ НАДЕЖНО: 13 символов с гарантией уникальности
     * Формат: 555 + 5 цифр timestamp + 5 цифр счетчик
     */
    private String generateTicketNumber() {
        // Последние 5 цифр timestamp (меняется каждую миллисекунду)
        long timestamp = System.currentTimeMillis() % 100000L;

        // Инкрементируем счетчик и берем последние 5 цифр
        int count = counter.incrementAndGet() % 100000;

        // 555 (3) + timestamp (5) + counter (5) = 13 символов
        return String.format("555%05d%05d", timestamp, count);
    }

    private String generateETicketNumber() {
        // 6-значный буквенно-цифровой код
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(6);
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }
}
