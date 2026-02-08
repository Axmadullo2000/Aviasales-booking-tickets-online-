package com.aviasales.booking.booking.service.impl;

import com.aviasales.booking.booking.dto.request.RoundTripSearchRequest;
import com.aviasales.booking.booking.dto.response.DynamicPriceResponse;
import com.aviasales.booking.booking.dto.response.RoundTripDiscountResponse;
import com.aviasales.booking.booking.dto.response.RoundTripSearchResponse;
import com.aviasales.booking.booking.entity.Flight;
import com.aviasales.booking.booking.enums.CabinClass;
import com.aviasales.booking.booking.enums.DemandLevel;
import com.aviasales.booking.booking.enums.TripType;
import com.aviasales.booking.booking.exception.FlightNotFoundException;
import com.aviasales.booking.booking.repo.FlightRepository;
import com.aviasales.booking.booking.service.interfaces.PricingService;
import com.aviasales.booking.booking.service.interfaces.RoundTripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для поиска билетов туда-обратно
 * Автоматически применяет скидку на round-trip бронирования
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoundTripServiceImpl implements RoundTripService {

    private final FlightRepository flightRepository;
    private final PricingService pricingService;

    /**
     * Поиск рейсов туда-обратно с рекомендациями
     */
    public RoundTripSearchResponse searchRoundTrip(RoundTripSearchRequest request) {
        log.info("Searching round-trip flights: {} -> {} ({}), return ({})",
                request.getFrom(), request.getTo(),
                request.getDepartureDate(), request.getReturnDate());

        LocalDate today = LocalDate.now();

        // Поиск рейсов туда
        List<Flight> outboundFlights = searchFlights(
                request.getFrom(),
                request.getTo(),
                request.getDepartureDate(),
                request.getFlexibleDates()
        );

        // Поиск рейсов обратно
        List<Flight> returnFlights = searchFlights(
                request.getTo(),
                request.getFrom(),
                request.getReturnDate(),
                request.getFlexibleDates()
        );

        log.info("Found {} outbound and {} return flights",
                outboundFlights.size(), returnFlights.size());

        // Конвертируем в FlightOption с динамическими ценами
        List<RoundTripSearchResponse.FlightOption> outboundOptions = convertToFlightOptions(
                outboundFlights,
                request.getCabinClass(),
                today
        );

        List<RoundTripSearchResponse.FlightOption> returnOptions = convertToFlightOptions(
                returnFlights,
                request.getCabinClass(),
                today
        );

        // Генерируем рекомендуемые комбинации
        List<RoundTripSearchResponse.RecommendedCombination> recommendations =
                generateRecommendations(outboundOptions, returnOptions);

        // Находим самую низкую цену туда-обратно
        BigDecimal lowestPrice = recommendations.isEmpty()
                ? BigDecimal.ZERO
                : recommendations.get(0).getTotalAfterDiscount();

        // Скидка на round-trip
        double discountPercent = TripType.ROUND_TRIP.getDiscount() * 100; // 5%

        return RoundTripSearchResponse.builder()
                .outboundFlights(outboundOptions)
                .returnFlights(returnOptions)
                .recommendations(recommendations)
                .lowestRoundTripPrice(lowestPrice)
                .roundTripDiscount(lowestPrice.multiply(BigDecimal.valueOf(TripType.ROUND_TRIP.getDiscount())))
                .discountPercent(discountPercent)
                .build();
    }

    /**
     * Поиск рейсов с опциональной гибкостью дат
     */
    public List<Flight> searchFlights(
            String origin,
            String destination,
            LocalDate date,
            Boolean flexibleDates
    ) {
        if (Boolean.TRUE.equals(flexibleDates)) {
            // Ищем ±3 дня
            LocalDate startDate = date.minusDays(3);
            LocalDate endDate = date.plusDays(3);

            return flightRepository.findFlightsByRouteAndDateRange(
                    origin,
                    destination,
                    startDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant(),
                    endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
            );
        } else {
            // Только выбранная дата
            return flightRepository.findFlightsByRouteAndDate(
                    origin,
                    destination,
                    date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant(),
                    date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
            );
        }
    }

    /**
     * Конвертировать Flight в FlightOption с динамическими ценами
     */
    public List<RoundTripSearchResponse.FlightOption> convertToFlightOptions(
            List<Flight> flights,
            CabinClass cabinClass,
            LocalDate bookingDate
    ) {
        return flights.stream()
                .map(flight -> {
                    DynamicPriceResponse priceInfo = pricingService.calculateDynamicPrice(
                            flight, cabinClass, bookingDate
                    );

                    Duration duration = Duration.between(
                            flight.getDepartureTime(),
                            flight.getArrivalTime()
                    );

                    return RoundTripSearchResponse.FlightOption.builder()
                            .flightId(flight.getId())
                            .flightNumber(flight.getFlightNumber())
                            .origin(flight.getOrigin().getIataCode())
                            .destination(flight.getDestination().getIataCode())
                            .departureTime(flight.getDepartureTime().toString())
                            .arrivalTime(flight.getArrivalTime().toString())
                            .duration(formatDuration(duration))
                            .airline(flight.getAirline().getName())
                            .basePrice(priceInfo.getBasePrice())
                            .dynamicPrice(priceInfo.getFinalPrice())
                            .availableSeats(getAvailableSeats(flight, cabinClass))
                            .demandLevel(String.valueOf(priceInfo.getDemandLevel()))
                            .priceReason(priceInfo.getRecommendation())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Генерировать рекомендуемые комбинации туда+обратно
     */
    public List<RoundTripSearchResponse.RecommendedCombination> generateRecommendations(
            List<RoundTripSearchResponse.FlightOption> outbound,
            List<RoundTripSearchResponse.FlightOption> returnFlights
    ) {
        List<RoundTripSearchResponse.RecommendedCombination> combinations = new ArrayList<>();

        // Создаем все возможные комбинации
        for (RoundTripSearchResponse.FlightOption out : outbound) {
            for (RoundTripSearchResponse.FlightOption ret : returnFlights) {
                BigDecimal totalBefore = out.getDynamicPrice().add(ret.getDynamicPrice());

                // Применяем скидку 5%
                BigDecimal discount = totalBefore.multiply(BigDecimal.valueOf(TripType.ROUND_TRIP.getDiscount()));
                BigDecimal totalAfter = totalBefore.subtract(discount);

                // Вычисляем общее время
                Duration outDuration = parseDuration(out.getDuration());
                Duration retDuration = parseDuration(ret.getDuration());
                String totalDuration = formatDuration(outDuration.plus(retDuration));

                // Причина рекомендации + рейтинг
                int score = calculateScore(out, ret, totalAfter);
                String reason = generateReason(score, out, ret);

                combinations.add(RoundTripSearchResponse.RecommendedCombination.builder()
                        .outboundFlightId(out.getFlightId())
                        .returnFlightId(ret.getFlightId())
                        .totalBeforeDiscount(totalBefore.setScale(2, RoundingMode.HALF_UP))
                        .totalAfterDiscount(totalAfter.setScale(2, RoundingMode.HALF_UP))
                        .savings(discount.setScale(2, RoundingMode.HALF_UP))
                        .totalDuration(totalDuration)
                        .reason(reason)
                        .score(score)
                        .build());
            }
        }

        // Сортируем по рейтингу (score) от большего к меньшему
        return combinations.stream()
                .sorted(Comparator.comparing(RoundTripSearchResponse.RecommendedCombination::getScore).reversed())
                .limit(10) // Топ-10 комбинаций
                .collect(Collectors.toList());
    }

    /**
     * Рассчитать рейтинг комбинации (0-100)
     */
    public int calculateScore(
            RoundTripSearchResponse.FlightOption out,
            RoundTripSearchResponse.FlightOption ret,
            BigDecimal totalPrice
    ) {
        int score = 50; // Базовый рейтинг

        // Бонус за низкую цену (макс +30)
        BigDecimal averagePrice = new BigDecimal("350"); // Средняя цена туда-обратно
        if (totalPrice.compareTo(averagePrice) < 0) {
            int priceBonus = averagePrice.subtract(totalPrice)
                    .divide(averagePrice, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(30))
                    .intValue();
            score += Math.min(priceBonus, 30);
        }

        // Бонус за доступность мест (макс +10)
        int totalSeats = out.getAvailableSeats() + ret.getAvailableSeats();
        if (totalSeats > 100) score += 10;
        else if (totalSeats > 50) score += 5;

        // Бонус за низкий спрос (макс +10)
        if ("LOW".equals(out.getDemandLevel()) && "LOW".equals(ret.getDemandLevel())) {
            score += 10;
        } else if ("LOW".equals(out.getDemandLevel()) || "LOW".equals(ret.getDemandLevel())) {
            score += 5;
        }

        // Штраф за высокий спрос (-10)
        if ("CRITICAL".equals(out.getDemandLevel()) || "CRITICAL".equals(ret.getDemandLevel())) {
            score -= 10;
        }

        return Math.max(0, Math.min(100, score)); // Ограничиваем 0-100
    }

    /**
     * Генерировать причину рекомендации
     */
    public String generateReason(
            int score,
            RoundTripSearchResponse.FlightOption out,
            RoundTripSearchResponse.FlightOption ret
    ) {
        if (score >= 80) {
            return "Best value - great price with good availability";
        } else if (score >= 70) {
            return "Recommended - balanced price and availability";
        } else if (score >= 60) {
            return "Good option - competitive pricing";
        } else if (score >= 50) {
            return "Standard option";
        } else {
            return "Limited availability or higher price";
        }
    }

    /**
     * Форматировать duration в читаемый вид
     */
    public String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%dh %dm", hours, minutes);
    }

    /**
     * Парсить строку duration обратно в Duration
     */
    public Duration parseDuration(String durationStr) {
        // "5h 30m" -> Duration
        String[] parts = durationStr.replace("h", "").replace("m", "").split(" ");
        long hours = Long.parseLong(parts[0]);
        long minutes = parts.length > 1 ? Long.parseLong(parts[1]) : 0;
        return Duration.ofHours(hours).plusMinutes(minutes);
    }

    /**
     * Получить доступные места
     */
    public Integer getAvailableSeats(
            Flight flight,
            CabinClass cabinClass
    ) {
        return switch (cabinClass) {
            case ECONOMY -> flight.getAvailableEconomy();
            case BUSINESS -> flight.getAvailableBusiness();
            case FIRST_CLASS -> flight.getAvailableFirstClass();
            default -> flight.getAvailableSeats();
        };
    }

    @Override
    public RoundTripDiscountResponse calculateDiscount(Long outboundFlightId, Long returnFlightId, CabinClass cabinClass, LocalDate bookingDate) {
        log.info("Calculating round-trip discount: outbound={}, return={}, class={}",
                outboundFlightId, returnFlightId, cabinClass);

        // 1. Получаем рейсы
        Flight outboundFlight = flightRepository.findById(outboundFlightId)
                .orElseThrow(() -> new FlightNotFoundException(outboundFlightId));

        Flight returnFlight = flightRepository.findById(returnFlightId)
                .orElseThrow(() -> new FlightNotFoundException(returnFlightId));

        // 2. Рассчитываем цены
        DynamicPriceResponse outboundPricing = pricingService.calculateDynamicPrice(
                outboundFlight, cabinClass, bookingDate
        );

        DynamicPriceResponse returnPricing = pricingService.calculateDynamicPrice(
                returnFlight, cabinClass, bookingDate
        );

        // 3. Цены до скидки
        BigDecimal outboundPrice = outboundPricing.getTotalPrice();
        BigDecimal returnPrice = returnPricing.getTotalPrice();
        BigDecimal totalBeforeDiscount = outboundPrice.add(returnPrice);

        // 4. Расчет скидки (5% для туда-обратно)
        BigDecimal discountPercent = new BigDecimal("5.0");
        BigDecimal discountAmount = totalBeforeDiscount
                .multiply(discountPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal totalAfterDiscount = totalBeforeDiscount.subtract(discountAmount);

        // 5. Проверка "хорошей сделки"
        boolean isGoodDeal = isGoodDeal(outboundPricing, returnPricing);

        // 6. Генерация рекомендации
        String recommendation = generateDiscountRecommendation(
                discountAmount,
                isGoodDeal,
                outboundPricing.getDemandLevel(),
                returnPricing.getDemandLevel()
        );

        // 7. Формирование ответа
        return RoundTripDiscountResponse.builder()
                .outboundFlight(createFlightSummary(outboundFlight, cabinClass))
                .returnFlight(createFlightSummary(returnFlight, cabinClass))
                .outboundPrice(outboundPrice)
                .returnPrice(returnPrice)
                .totalPriceBeforeDiscount(totalBeforeDiscount)
                .discountPercent(discountPercent)
                .discountAmount(discountAmount)
                .totalPriceAfterDiscount(totalAfterDiscount)
                .totalSavings(discountAmount)
                .discountMessage("5% discount applied for round-trip booking")
                .isGoodDeal(isGoodDeal)
                .recommendation(recommendation)
                .build();
    }

    /**
     * Создать краткую информацию о рейсе
     */
    private RoundTripDiscountResponse.FlightSummary createFlightSummary(
            Flight flight,
            CabinClass cabinClass
    ) {
        Duration duration = Duration.between(
                flight.getDepartureTime(),
                flight.getArrivalTime()
        );

        String route = flight.getOrigin().getIataCode() + " → " +
                flight.getDestination().getIataCode();

        return RoundTripDiscountResponse.FlightSummary.builder()
                .flightId(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .route(route)
                .departureTime(flight.getDepartureTime().toString())
                .arrivalTime(flight.getArrivalTime().toString())
                .duration(formatDuration(duration))
                .airline(flight.getAirline().getName())
                .availableSeats(pricingService.getAvailableSeatsForClass(flight, cabinClass))
                .build();
    }

    /**
     * Проверить является ли это хорошей сделкой
     */
    private boolean isGoodDeal(DynamicPriceResponse outbound, DynamicPriceResponse inbound) {
        // Хорошая сделка если:
        // - Оба рейса с низким или средним спросом
        // - ИЛИ раннее бронирование (>30 дней)
        boolean lowDemand = (outbound.getDemandLevel() == DemandLevel.LOW ||
                outbound.getDemandLevel() == DemandLevel.MEDIUM) &&
                (inbound.getDemandLevel() == DemandLevel.LOW ||
                        inbound.getDemandLevel() == DemandLevel.MEDIUM);

        boolean earlyBooking = outbound.getDaysUntilDeparture() > 30 ||
                inbound.getDaysUntilDeparture() > 30;

        return lowDemand || earlyBooking;
    }

    /**
     * Генерировать рекомендацию
     */
    private String generateDiscountRecommendation(
            BigDecimal savings,
            boolean isGoodDeal,
            DemandLevel outboundDemand,
            DemandLevel returnDemand
    ) {
        if (isGoodDeal) {
            return String.format(
                    "Excellent deal! Save $%.2f with round-trip booking. Book now!",
                    savings
            );
        } else if (outboundDemand == DemandLevel.VERY_HIGH ||
                returnDemand == DemandLevel.VERY_HIGH) {
            return String.format(
                    "High demand detected. Still save $%.2f with round-trip - book soon!",
                    savings
            );
        } else {
            return String.format(
                    "Save $%.2f by booking round-trip instead of separate tickets",
                    savings
            );
        }
    }
}
