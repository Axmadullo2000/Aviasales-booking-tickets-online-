package com.aviasales.booking.booking.service.interfaces;

import com.aviasales.booking.booking.dto.request.RoundTripSearchRequest;
import com.aviasales.booking.booking.dto.response.RoundTripDiscountResponse;
import com.aviasales.booking.booking.dto.response.RoundTripSearchResponse;
import com.aviasales.booking.booking.entity.Flight;
import com.aviasales.booking.booking.enums.CabinClass;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;


public interface RoundTripService {

    /**
     * Поиск рейсов туда-обратно с рекомендациями
     */
    RoundTripSearchResponse searchRoundTrip(RoundTripSearchRequest request);
    /**
     * Поиск рейсов с опциональной гибкостью дат
     */
    List<Flight> searchFlights(
            String origin,
            String destination,
            LocalDate date,
            Boolean flexibleDates
    );

    /**
     * Конвертировать Flight в FlightOption с динамическими ценами
     */
    List<RoundTripSearchResponse.FlightOption> convertToFlightOptions(
            List<Flight> flights,
            CabinClass cabinClass,
            LocalDate bookingDate
    );

    /**
     * Генерировать рекомендуемые комбинации туда+обратно
     */
    List<RoundTripSearchResponse.RecommendedCombination> generateRecommendations(
            List<RoundTripSearchResponse.FlightOption> outbound,
            List<RoundTripSearchResponse.FlightOption> returnFlights
    );

    /**
     * Рассчитать рейтинг комбинации (0-100)
     */
    int calculateScore(
            RoundTripSearchResponse.FlightOption out,
            RoundTripSearchResponse.FlightOption ret,
            BigDecimal totalPrice
    );

    /**
     * Генерировать причину рекомендации
     */
    String generateReason(
            int score,
            RoundTripSearchResponse.FlightOption out,
            RoundTripSearchResponse.FlightOption ret
    );

    /**
     * Форматировать duration в читаемый вид
     */
    String formatDuration(Duration duration);

    /**
     * Парсить строку duration обратно в Duration
     */
    Duration parseDuration(String durationStr);

    /**
     * Получить доступные места
     */
    Integer getAvailableSeats(
            Flight flight,
            CabinClass cabinClass
    );

    /**
     * Рассчитать скидку для комбинации рейсов туда-обратно
     */
    RoundTripDiscountResponse calculateDiscount(
            Long outboundFlightId,
            Long returnFlightId,
            CabinClass cabinClass,
            LocalDate bookingDate
    );
}

