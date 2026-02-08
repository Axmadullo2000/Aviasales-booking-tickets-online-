package com.aviasales.booking.booking.service.impl;

import com.aviasales.booking.booking.dto.response.CalendarPriceResponse;
import com.aviasales.booking.booking.dto.response.DynamicPriceResponse;
import com.aviasales.booking.booking.entity.Flight;
import com.aviasales.booking.booking.entity.Holiday;
import com.aviasales.booking.booking.enums.CabinClass;
import com.aviasales.booking.booking.repo.FlightRepository;
import com.aviasales.booking.booking.repo.HolidayRepository;
import com.aviasales.booking.booking.service.interfaces.CalendarService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Сервис для работы с календарем цен
 * Показывает пользователю цены на весь месяц
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    private final FlightRepository flightRepository;
    private final HolidayRepository holidayRepository;
    private final PricingServiceImpl pricingServiceImpl;

    /**
     * Получить календарь цен на месяц
     */
    public CalendarPriceResponse getMonthlyPrices(
            String originCode,
            String destinationCode,
            YearMonth month,
            CabinClass cabinClass
    ) {
        log.info("Fetching calendar prices for route {}->{} for month {}",
                originCode, destinationCode, month);

        // Получаем все дни месяца
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        // ✅ Конвертируем LocalDate в Instant (UTC)
        Instant startInstant = startDate.atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant endInstant = endDate.atTime(23, 59, 59).atZone(ZoneId.of("UTC")).toInstant();

        // ✅ Используем существующий метод из репозитория
        List<Flight> monthFlights = flightRepository.findByOriginIataCodeAndDestinationIataCodeAndDepartureTimeBetween(
                originCode,
                destinationCode,
                startInstant,
                endInstant
        );

        // Получаем праздники на этот месяц
        List<Holiday> holidays = holidayRepository.findHolidaysInRange(startDate, endDate, "UZ");
        Map<LocalDate, Holiday> holidayMap = holidays.stream()
                .collect(Collectors.toMap(Holiday::getHolidayDate, h -> h));

        // ✅ Группируем рейсы по локальной дате (используем метод из Flight entity)
        Map<LocalDate, List<Flight>> flightsByDate = monthFlights.stream()
                .filter(f -> f.getDepartureDateLocal() != null)
                .collect(Collectors.groupingBy(Flight::getDepartureDateLocal));
        // Создаем цены для каждого дня
        List<CalendarPriceResponse.DayPrice> dayPrices = new ArrayList<>();
        LocalDate cheapestDay = null;
        BigDecimal lowestPrice = null;
        BigDecimal totalPrices = BigDecimal.ZERO;
        int daysWithFlights = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<Flight> dayFlights = flightsByDate.getOrDefault(date, Collections.emptyList());

            if (dayFlights.isEmpty()) {
                // Нет рейсов в этот день
                continue;
            }

            // Находим минимальную цену среди всех рейсов дня
            BigDecimal minPrice = null;
            Integer totalAvailableSeats = 0;
            String demandLevel = "LOW";

            for (Flight flight : dayFlights) {
                DynamicPriceResponse priceInfo = pricingServiceImpl.calculateDynamicPrice(
                        flight, cabinClass, LocalDate.now()
                );

                if (minPrice == null || priceInfo.getFinalPrice().compareTo(minPrice) < 0) {
                    minPrice = priceInfo.getFinalPrice();
                    demandLevel = String.valueOf(priceInfo.getDemandLevel());
                }

                totalAvailableSeats += getAvailableSeats(flight, cabinClass);
            }

            // Отслеживаем самый дешевый день
            if (minPrice != null) {
                if (lowestPrice == null || minPrice.compareTo(lowestPrice) < 0) {
                    lowestPrice = minPrice;
                    cheapestDay = date;
                }

                totalPrices = totalPrices.add(minPrice);
                daysWithFlights++;
            }

            // Информация о дне
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
            Holiday holiday = holidayMap.get(date);

            CalendarPriceResponse.DayPrice dayPrice = CalendarPriceResponse.DayPrice.builder()
                    .date(date)
                    .minPrice(minPrice)
                    .availableSeats(totalAvailableSeats)
                    .dayOfWeek(dayOfWeek.toString())
                    .isWeekend(isWeekend)
                    .isHoliday(holiday != null)
                    .holidayName(holiday != null ? holiday.getName() : null)
                    .isCheapest(false) // Установим позже
                    .priceReason(getPriceReason(date, isWeekend, holiday, demandLevel))
                    .demandLevel(demandLevel)
                    .flightCount(dayFlights.size())
                    .build();

            dayPrices.add(dayPrice);
        }

        // Помечаем самый дешевый день
        if (cheapestDay != null) {
            LocalDate finalCheapestDay = cheapestDay;
            dayPrices.stream()
                    .filter(dp -> dp.getDate().equals(finalCheapestDay))
                    .findFirst()
                    .ifPresent(dp -> dp.setIsCheapest(true));
        }

        // Средняя цена
        BigDecimal averagePrice = daysWithFlights > 0
                ? totalPrices.divide(BigDecimal.valueOf(daysWithFlights), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return CalendarPriceResponse.builder()
                .month(month.toString())
                .route(originCode + " → " + destinationCode)
                .prices(dayPrices)
                .cheapestDay(cheapestDay)
                .averagePrice(averagePrice)
                .build();
    }

    /**
     * Получить доступные места по классу
     */
    public Integer getAvailableSeats(Flight flight, CabinClass cabinClass) {
        return switch (cabinClass) {
            case ECONOMY -> flight.getAvailableEconomy();
            case BUSINESS -> flight.getAvailableBusiness();
            case FIRST_CLASS -> flight.getAvailableFirstClass();
            default -> flight.getAvailableSeats();
        };
    }

    /**
     * Объяснение цены
     */
    public String getPriceReason(LocalDate date, boolean isWeekend, Holiday holiday, String demandLevel) {
        if (holiday != null) {
            return "Holiday: " + holiday.getName();
        }

        if (isWeekend) {
            return "Weekend pricing";
        }

        if ("CRITICAL".equals(demandLevel)) {
            return "High demand - limited seats";
        }

        if ("HIGH".equals(demandLevel)) {
            return "Popular date";
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.MONDAY) {
            return "Popular travel day";
        }

        return "Standard pricing";
    }
}
