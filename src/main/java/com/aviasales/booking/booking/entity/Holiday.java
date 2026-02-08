package com.aviasales.booking.booking.entity;


import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Праздничные дни с повышенным спросом
 * Используется для динамического ценообразования
 */
@Entity
@Table(name = "holidays", indexes = {
        @Index(name = "idx_holiday_date", columnList = "holiday_date"),
        @Index(name = "idx_holiday_country", columnList = "country")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Код страны (ISO 3166-1 alpha-2)
     * UZ - Узбекистан
     * RU - Россия
     * US - США
     */
    @Column(name = "country", nullable = false, length = 2)
    private String country;

    /**
     * Дата праздника
     */
    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    /**
     * Название праздника
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Множитель цены для этого праздника
     * По умолчанию 1.3 (наценка 30%)
     */
    @Column(name = "multiplier", precision = 4, scale = 2)
    private BigDecimal multiplier = new BigDecimal("1.30");

    /**
     * Активен ли праздник для ценообразования
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * Количество дней до/после когда действует повышенная цена
     * Например: за 3 дня до Нового года - тоже дорого
     */
    @Column(name = "days_range")
    private Integer daysRange = 0;

    /**
     * Описание или примечания
     */
    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (multiplier == null) {
            multiplier = new BigDecimal("1.30");
        }
        if (isActive == null) {
            isActive = true;
        }
        if (daysRange == null) {
            daysRange = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Проверяет попадает ли дата в диапазон действия праздника
     */
    public boolean isInRange(LocalDate date) {
        if (!isActive) {
            return false;
        }

        LocalDate rangeStart = holidayDate.minusDays(daysRange);
        LocalDate rangeEnd = holidayDate.plusDays(daysRange);

        return !date.isBefore(rangeStart) && !date.isAfter(rangeEnd);
    }
}
