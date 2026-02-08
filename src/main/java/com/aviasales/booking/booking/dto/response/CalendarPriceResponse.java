package com.aviasales.booking.booking.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Ответ с ценами на календарный месяц
 * Используется для отображения календаря цен пользователю
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarPriceResponse {

    /**
     * Месяц в формате YYYY-MM
     */
    private String month;

    /**
     * Маршрут
     */
    private String route;

    /**
     * Цены по дням
     */
    private List<DayPrice> prices;

    /**
     * Самый дешёвый день
     */
    private LocalDate cheapestDay;

    /**
     * Средняя цена за месяц
     */
    private BigDecimal averagePrice;

    /**
     * Информация о цене на конкретный день
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DayPrice {
        /**
         * Дата
         */
        private LocalDate date;

        /**
         * Минимальная цена на этот день
         */
        private BigDecimal minPrice;

        /**
         * Доступно мест
         */
        private Integer availableSeats;

        /**
         * День недели (MONDAY, FRIDAY, etc)
         */
        private String dayOfWeek;

        /**
         * Это выходной?
         */
        private Boolean isWeekend;

        /**
         * Это праздник?
         */
        private Boolean isHoliday;

        /**
         * Название праздника (если есть)
         */
        private String holidayName;

        /**
         * Самый дешёвый день?
         */
        private Boolean isCheapest;

        /**
         * Причина высокой/низкой цены
         */
        private String priceReason;

        /**
         * Уровень спроса
         */
        private String demandLevel;

        /**
         * Количество рейсов на эту дату
         */
        private Integer flightCount;
    }
}
