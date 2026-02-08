package com.aviasales.booking.booking.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Ответ с результатами поиска туда-обратно
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoundTripSearchResponse {

    /**
     * Рейсы туда
     */
    private List<FlightOption> outboundFlights;

    /**
     * Рейсы обратно
     */
    private List<FlightOption> returnFlights;

    /**
     * Рекомендуемые комбинации
     */
    private List<RecommendedCombination> recommendations;

    /**
     * Самая низкая цена туда-обратно
     */
    private BigDecimal lowestRoundTripPrice;

    /**
     * Скидка на обратный билет
     */
    private BigDecimal roundTripDiscount;

    /**
     * Процент скидки
     */
    private Double discountPercent;

    /**
     * Опция рейса с динамической ценой
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FlightOption {
        private Long flightId;
        private String flightNumber;
        private String origin;
        private String destination;
        private String departureTime;
        private String arrivalTime;
        private String duration;
        private String airline;

        /**
         * Базовая цена
         */
        private BigDecimal basePrice;

        /**
         * Динамическая цена (с учётом всех коэффициентов)
         */
        private BigDecimal dynamicPrice;

        /**
         * Доступно мест
         */
        private Integer availableSeats;

        /**
         * Уровень спроса
         */
        private String demandLevel;

        /**
         * Почему такая цена
         */
        private String priceReason;
    }

    /**
     * Рекомендуемая комбинация туда+обратно
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendedCombination {
        /**
         * ID рейса туда
         */
        private Long outboundFlightId;

        /**
         * ID рейса обратно
         */
        private Long returnFlightId;

        /**
         * Общая цена до скидки
         */
        private BigDecimal totalBeforeDiscount;

        /**
         * Общая цена после скидки
         */
        private BigDecimal totalAfterDiscount;

        /**
         * Сумма скидки
         */
        private BigDecimal savings;

        /**
         * Общее время в пути (туда + обратно)
         */
        private String totalDuration;

        /**
         * Почему рекомендуем
         */
        private String reason;

        /**
         * Рейтинг комбинации (0-100)
         */
        private Integer score;
    }
}
