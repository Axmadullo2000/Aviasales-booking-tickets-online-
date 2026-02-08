package com.aviasales.booking.booking.service.interfaces;

import com.aviasales.booking.booking.dto.response.DynamicPriceResponse;
import com.aviasales.booking.booking.entity.Flight;
import com.aviasales.booking.booking.enums.CabinClass;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PricingService {

    /**
     * Рассчитать динамическую цену для рейса
     */
    DynamicPriceResponse calculateDynamicPrice(
            Flight flight,
            CabinClass cabinClass,
            LocalDate bookingDate
    );

    /**
     * Получить доступные места для класса
     */
    int getAvailableSeatsForClass(Flight flight, CabinClass cabinClass);

    /**
     * Проверить достаточность мест
     */
    boolean hasEnoughSeats(Flight flight, CabinClass cabinClass, int requiredSeats);

    /**
     * Рассчитать общую стоимость для нескольких пассажиров
     */
    BigDecimal calculateTotalPrice(
            Flight flight,
            CabinClass cabinClass,
            int passengerCount,
            LocalDate bookingDate
    );
}
