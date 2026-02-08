package com.aviasales.booking.booking.enums;

import lombok.Getter;

/**
 * Тип поездки с автоматическими скидками
 */
@Getter
public enum TripType {
    /**
     * Билет в одну сторону - без скидки
     */
    ONE_WAY(0.0),

    /**
     * Билет туда-обратно - скидка 5%
     */
    ROUND_TRIP(0.05),

    /**
     * Мультисити (несколько городов) - скидка 3%
     */
    MULTI_CITY(0.03);

    /**
     * Скидка в виде десятичной дроби (0.05 = 5%)
     */
    private final double discount;

    TripType(double discount) {
        this.discount = discount;
    }

    /**
     * Получить скидку в процентах
     */
    public double getDiscountPercent() {
        return discount * 100;
    }
}
