package com.aviasales.booking.booking.entity;

import com.aviasales.booking.booking.enums.CabinClass;
import com.aviasales.booking.booking.enums.FlightStatus;
import com.aviasales.booking.booking.exception.InsufficientSeatsException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "flights", indexes = {
        @Index(name = "idx_route_date", columnList = "origin_id, destination_id, departure_time"),
        @Index(name = "idx_flight_number", columnList = "flight_number, departure_time"),
        @Index(name = "idx_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class Flight implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ═══════════════════════════════════════
    // ОСНОВНАЯ ИНФОРМАЦИЯ
    // ═══════════════════════════════════════

    @Column(nullable = false, length = 10)
    private String flightNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airline_id", nullable = false)
    private Airline airline;

    // ═══════════════════════════════════════
    // МАРШРУТ
    // ═══════════════════════════════════════

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_id", nullable = false)
    private Airport origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Airport destination;

    // ═══════════════════════════════════════
    // ВРЕМЯ (UTC!) - КРИТИЧЕСКИ ВАЖНО
    // ═══════════════════════════════════════

    /**
     * Время вылета в UTC
     * Для отображения пользователю конвертируется в timezone аэропорта вылета
     */
    @Column(nullable = false)
    private Instant departureTime;  // ✅ Instant (UTC)

    /**
     * Время прилёта в UTC
     * Для отображения пользователю конвертируется в timezone аэропорта прилёта
     */
    @Column(nullable = false)
    private Instant arrivalTime;  // ✅ Instant (UTC)

    /**
     * Длительность полёта в минутах
     * Рассчитывается автоматически как разница между arrivalTime и departureTime
     */
    private Integer durationMinutes;

    // ═══════════════════════════════════════
    // ЦЕНООБРАЗОВАНИЕ
    // ═══════════════════════════════════════

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal businessPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal firstClassPrice;

    // ═══════════════════════════════════════
    // ВМЕСТИМОСТЬ
    // ═══════════════════════════════════════

    @Column(nullable = false)
    private Integer totalSeats;

    private Integer economySeats;
    private Integer businessSeats;
    private Integer firstClassSeats;

    // Доступные

    @Builder.Default
    private Integer availableEconomy = 0;

    @Builder.Default
    private Integer availableBusiness = 0;

    @Builder.Default
    private Integer availableFirstClass = 0;

    @Builder.Default
    private Integer availableSeats = 0;

    // ═══════════════════════════════════════
    // СТАТУС РЕЙСА
    // ═══════════════════════════════════════

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FlightStatus status = FlightStatus.SCHEDULED;

    private String aircraftType;

    private Integer stops;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_via")
    private Airport connectionAirport;

    // ═══════════════════════════════════════
    // АУДИТ
    // ═══════════════════════════════════════

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // ═══════════════════════════════════════
    // БИЗНЕС-ЛОГИКА
    // ═══════════════════════════════════════

    public boolean isBookable() {
        return status == FlightStatus.SCHEDULED
                && availableSeats > 0
                && departureTime.isAfter(Instant.now().plusSeconds(2 * 3600));  // минимум 2 часа до вылета
    }

    public void reserveSeats(int count, CabinClass cabinClass) throws InsufficientSeatsException {
        switch (cabinClass) {
            case ECONOMY -> {
                if (availableEconomy == null || availableEconomy < count) {
                    throw new InsufficientSeatsException(
                            String.format("Not enough economy seats. Requested: %d, Available: %d",
                                    count, availableEconomy != null ? availableEconomy : 0)
                    );
                }
                availableEconomy -= count;
            }
            case BUSINESS -> {
                if (availableBusiness == null || availableBusiness < count) {
                    throw new InsufficientSeatsException(
                            String.format("Not enough business seats. Requested: %d, Available: %d",
                                    count, availableBusiness != null ? availableBusiness : 0)
                    );
                }
                availableBusiness -= count;
            }
            case FIRST_CLASS -> {
                if (availableFirstClass == null || availableFirstClass < count) {
                    throw new InsufficientSeatsException(
                            String.format("Not enough first class seats. Requested: %d, Available: %d",
                                    count, availableFirstClass != null ? availableFirstClass : 0)
                    );
                }
                availableFirstClass -= count;
            }
        }
        this.availableSeats -= count;
    }

    /**
     * Освобождает места с учетом класса кабины
     */
    public void releaseSeats(int count, CabinClass cabinClass) {
        switch (cabinClass) {
            case ECONOMY -> {
                availableEconomy = Math.min(
                        (availableEconomy != null ? availableEconomy : 0) + count,
                        economySeats != null ? economySeats : 0
                );
            }
            case BUSINESS -> {
                availableBusiness = Math.min(
                        (availableBusiness != null ? availableBusiness : 0) + count,
                        businessSeats != null ? businessSeats : 0
                );
            }
            case FIRST_CLASS -> {
                availableFirstClass = Math.min(
                        (availableFirstClass != null ? availableFirstClass : 0) + count,
                        firstClassSeats != null ? firstClassSeats : 0
                );
            }
        }
        this.availableSeats = Math.min(
                (this.availableSeats != null ? this.availableSeats : 0) + count,
                this.totalSeats != null ? this.totalSeats : 0
        );
    }

    /**
     * DEPRECATED: используйте releaseSeats(int count, CabinClass cabinClass)
     * Оставлено для обратной совместимости
     */
    @Deprecated
    public void releaseSeats(int count) {
        this.availableSeats = Math.min(
                (this.availableSeats != null ? this.availableSeats : 0) + count,
                this.totalSeats != null ? this.totalSeats : 0
        );
    }

    @PrePersist
    @PreUpdate
    public void onPrePersistOrUpdate() {
        // Валидация времени
        if (departureTime != null && arrivalTime != null) {
            if (!arrivalTime.isAfter(departureTime)) {
                throw new IllegalStateException(
                        "Arrival time must be after departure time"
                );
            }

            // Расчёт длительности
            this.durationMinutes = (int) Duration.between(
                    departureTime, arrivalTime
            ).toMinutes();
        }

        // ✅ ИНИЦИАЛИЗАЦИЯ ДОСТУПНЫХ МЕСТ (если null)
        if (availableEconomy == null && economySeats != null) {
            availableEconomy = economySeats;
        }
        if (availableBusiness == null && businessSeats != null) {
            availableBusiness = businessSeats;
        }
        if (availableFirstClass == null && firstClassSeats != null) {
            availableFirstClass = firstClassSeats;
        }

        // ✅ СИНХРОНИЗАЦИЯ totalSeats и availableSeats
        if (totalSeats == null) {
            totalSeats = (economySeats != null ? economySeats : 0)
                    + (businessSeats != null ? businessSeats : 0)
                    + (firstClassSeats != null ? firstClassSeats : 0);
        }

        if (availableSeats == null) {
            availableSeats = totalSeats;
        }

        // ✅ ВАЛИДАЦИЯ: доступные места не могут быть больше общего количества
        if (availableEconomy != null && economySeats != null && availableEconomy > economySeats) {
            availableEconomy = economySeats;
        }
        if (availableBusiness != null && businessSeats != null && availableBusiness > businessSeats) {
            availableBusiness = businessSeats;
        }
        if (availableFirstClass != null && firstClassSeats != null && availableFirstClass > firstClassSeats) {
            availableFirstClass = firstClassSeats;
        }
    }

    // ═══════════════════════════════════════
    // МЕТОДЫ ДЛЯ РАБОТЫ С ЛОКАЛЬНЫМ ВРЕМЕНЕМ
    // ═══════════════════════════════════════

    /**
     * Получить время вылета в локальном времени аэропорта вылета
     * Пример: Москва DME, timezone "Europe/Moscow", вылет 10:30
     */
    public ZonedDateTime getDepartureTimeLocal() {
        if (departureTime == null || origin == null || origin.getTimezone() == null) {
            return null;
        }
        ZoneId zoneId = ZoneId.of(origin.getTimezone());
        return departureTime.atZone(zoneId);
    }

    /**
     * Получить время прилёта в локальном времени аэропорта прилёта
     * Пример: Дубай DXB, timezone "Asia/Dubai", прилёт 16:45
     */
    public ZonedDateTime getArrivalTimeLocal() {
        if (arrivalTime == null || destination == null || destination.getTimezone() == null) {
            return null;
        }
        ZoneId zoneId = ZoneId.of(destination.getTimezone());
        return arrivalTime.atZone(zoneId);
    }

    /**
     * Получить дату вылета в локальном времени (для группировки рейсов по датам)
     */
    public LocalDate getDepartureDateLocal() {
        ZonedDateTime localTime = getDepartureTimeLocal();
        return localTime != null ? localTime.toLocalDate() : null;
    }

    /**
     * Получить часы до вылета
     */
    public long getHoursUntilDeparture() {
        return Duration.between(Instant.now(), departureTime).toHours();
    }

    /**
     * Получить процент заполненности
     */
    public double getOccupancyRate() {
        if (totalSeats == null || totalSeats == 0) {
            return 0.0;
        }
        int occupiedSeats = totalSeats - availableSeats;
        return (double) occupiedSeats / totalSeats * 100;
    }

    public boolean isAlmostFull() {
        return getOccupancyRate() > 80.0;
    }
}
