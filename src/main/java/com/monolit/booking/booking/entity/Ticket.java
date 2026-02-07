package com.monolit.booking.booking.entity;

import com.monolit.booking.booking.enums.CabinClass;
import com.monolit.booking.booking.enums.FareType;
import com.monolit.booking.booking.enums.SeatPreference;
import com.monolit.booking.booking.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // ✅ добавил GeneratedValue
    private Long id;

    // ═══════════════════════════════════════
    // ИДЕНТИФИКАЦИЯ
    // ═══════════════════════════════════════

    @Column(unique = true, length = 13)
    private String ticketNumber;  // ✅ 13-значный номер (стандарт IATA)

    @Column(unique = true, length = 6)
    private String eTicketNumber;  // ✅ электронный билет (для check-in)

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
    private CabinClass cabinClass;  // ✅ ECONOMY, BUSINESS, FIRST_CLASS

    private String seatNumber;  // 12A, 34F (может быть NULL до check-in)

    @Enumerated(EnumType.STRING)
    private SeatPreference seatPreference;  // ✅ WINDOW, AISLE, MIDDLE

    // ═══════════════════════════════════════
    // ЦЕНА И ТАРИФЫ
    // ═══════════════════════════════════════

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;  // ✅ общая цена билета

    @Column(precision = 10, scale = 2)
    private BigDecimal baseFare;  // ✅ тариф без налогов

    @Column(precision = 10, scale = 2)
    private BigDecimal taxes;  // ✅ налоги и сборы

    @Column(precision = 10, scale = 2)
    private BigDecimal serviceFee;  // ✅ комиссия сервиса

    @Enumerated(EnumType.STRING)
    private FareType fareType;  // ✅ ECONOMY_LIGHT, ECONOMY_STANDARD, BUSINESS

    // ═══════════════════════════════════════
    // БАГАЖ
    // ═══════════════════════════════════════

    private Integer checkedBaggage;  // ✅ кг багажа (0 для лоукостеров)
    private Integer handLuggage;     // ✅ кг ручной клади

    private Boolean hasPriorityBoarding;  // ✅ приоритетная посадка

    // ═══════════════════════════════════════
    // СТАТУС БИЛЕТА
    // ═══════════════════════════════════════

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    private LocalDateTime issuedAt;      // ✅ когда выписан
    private LocalDateTime checkedInAt;   // ✅ когда прошёл check-in
    private LocalDateTime boardedAt;     // ✅ когда сел в самолёт

    // ═══════════════════════════════════════
    // ИЗМЕНЕНИЯ И ВОЗВРАТЫ
    // ═══════════════════════════════════════

    private Boolean isRefundable;      // ✅ можно ли вернуть
    private Boolean isChangeable;      // ✅ можно ли изменить

    @Column(precision = 10, scale = 2)
    private BigDecimal changeFee;      // ✅ стоимость изменения

    @Column(precision = 10, scale = 2)
    private BigDecimal cancellationFee;  // ✅ стоимость отмены

    // ═══════════════════════════════════════
    // АУДИТ
    // ═══════════════════════════════════════

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

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

    private String generateTicketNumber() {
        // 13-значный номер IATA (555-1234567890)
        return "555" + String.format("%010d", System.currentTimeMillis() % 10000000000L);
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
