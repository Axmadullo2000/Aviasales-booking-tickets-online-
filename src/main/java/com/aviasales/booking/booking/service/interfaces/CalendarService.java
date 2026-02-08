package com.aviasales.booking.booking.service.interfaces;

import com.aviasales.booking.booking.dto.response.CalendarPriceResponse;
import com.aviasales.booking.booking.entity.Flight;
import com.aviasales.booking.booking.entity.Holiday;
import com.aviasales.booking.booking.enums.CabinClass;

import java.time.LocalDate;
import java.time.YearMonth;


/**
 * Интерфейс сервиса календаря цен
 */
public interface CalendarService {
    /**
     * Получить календарь цен на месяц по маршруту и классу обслуживания
     *
     * @param originCode      IATA код аэропорта вылета
     * @param destinationCode IATA код аэропорта назначения
     * @param month           Месяц (YearMonth)
     * @param cabinClass      Класс обслуживания
     * @return Календарь цен
     */

    CalendarPriceResponse getMonthlyPrices(
            String originCode,
            String destinationCode,
            YearMonth month,
            CabinClass cabinClass
    );

    Integer getAvailableSeats(Flight flight, CabinClass cabinClass);

    String getPriceReason(LocalDate date, boolean isWeekend, Holiday holiday, String demandLevel);

}
