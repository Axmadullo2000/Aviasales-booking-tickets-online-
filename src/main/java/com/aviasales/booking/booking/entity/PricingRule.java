package com.aviasales.booking.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Правила динамического ценообразования
 * Определяет как изменяется цена в зависимости от различных факторов
 */
@Entity
@Table(name = "pricing_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Тип правила
     * ADVANCE_PURCHASE - время до вылета
     * OCCUPANCY - загруженность рейса
     * DAY_OF_WEEK - день недели
     * HOLIDAY - праздники
     */
    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType;

    /**
     * Ключ условия
     * Примеры: "days_before", "occupancy_percent", "day_of_week"
     */
    @Column(name = "condition_key", nullable = false, length = 100)
    private String conditionKey;

    /**
     * Значение условия
     * Примеры: "60+", "70-85", "FRIDAY", "2026-03-21"
     */
    @Column(name = "condition_value", nullable = false, length = 50)
    private String conditionValue;

    /**
     * Множитель цены
     * 0.8 = скидка 20%
     * 1.0 = базовая цена
     * 1.5 = наценка 50%
     */
    @Column(name = "multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal multiplier;

    /**
     * Приоритет применения правила (чем выше - тем раньше)
     */
    @Column(name = "priority", nullable = false)
    private Integer priority;

    /**
     * Активно ли правило
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * Описание правила
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
