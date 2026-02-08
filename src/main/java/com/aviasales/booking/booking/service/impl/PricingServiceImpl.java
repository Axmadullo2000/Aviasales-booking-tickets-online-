package com.aviasales.booking.booking.service.impl;

import com.aviasales.booking.booking.dto.response.DynamicPriceResponse;
import com.aviasales.booking.booking.entity.Flight;
import com.aviasales.booking.booking.enums.CabinClass;
import com.aviasales.booking.booking.enums.DemandLevel;
import com.aviasales.booking.booking.service.interfaces.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

    // ═══════════════════════════════════════
    // ОСНОВНОЙ МЕТОД РАСЧЁТА ДИНАМИЧЕСКОЙ ЦЕНЫ
    // ═══════════════════════════════════════

    @Override
    public DynamicPriceResponse calculateDynamicPrice(
            Flight flight,
            CabinClass cabinClass,
            LocalDate bookingDate
    ) {
        log.debug("Calculating dynamic price for flight: {}, class: {}, booking date: {}",
                flight.getFlightNumber(), cabinClass, bookingDate);

        // 1️⃣ Базовая цена
        BigDecimal basePrice = getBasePrice(flight, cabinClass);

        // 2️⃣ Загруженность
        int occupancyPercent = calculateOccupancyPercent(flight, cabinClass);
        BigDecimal occupancyMultiplier = getOccupancyMultiplier(flight, cabinClass);

        // 3️⃣ Время до вылета
        long daysUntilDeparture = calculateDaysUntilDeparture(flight, bookingDate);
        BigDecimal timeMultiplier = getTimeMultiplier(daysUntilDeparture);

        // 4️⃣ День недели
        BigDecimal dayOfWeekMultiplier = getDayOfWeekMultiplier(flight);

        // 5️⃣ Уровень спроса
        DemandLevel demandLevel = calculateDemandLevel(occupancyPercent, daysUntilDeparture);

        // 6️⃣ Рекомендация
        String recommendation = generateRecommendation(demandLevel, occupancyPercent, daysUntilDeparture);

        // 7️⃣ Финальная цена
        BigDecimal finalPrice = basePrice
                .multiply(occupancyMultiplier)
                .multiply(timeMultiplier)
                .multiply(dayOfWeekMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        // 8️⃣ Расчет налогов и сборов
        BigDecimal taxes = finalPrice.multiply(new BigDecimal("0.15"))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalPrice = finalPrice.add(taxes);

        log.debug("Price calculation result: base={}, occupancy={}%, time={}d, demand={}, final={}",
                basePrice, occupancyPercent, daysUntilDeparture, demandLevel, finalPrice);

        return DynamicPriceResponse.builder()
                .basePrice(basePrice)
                .finalPrice(finalPrice)
                .taxes(taxes)
                .totalPrice(totalPrice)
                .occupancyPercent(occupancyPercent)
                .daysUntilDeparture(daysUntilDeparture)
                .occupancyMultiplier(occupancyMultiplier)
                .timeMultiplier(timeMultiplier)
                .dayOfWeekMultiplier(dayOfWeekMultiplier)
                .demandLevel(demandLevel)
                .recommendation(recommendation)
                .build();
    }

    // ═══════════════════════════════════════
    // ПОЛУЧЕНИЕ БАЗОВОЙ ЦЕНЫ
    // ═══════════════════════════════════════

    private BigDecimal getBasePrice(Flight flight, CabinClass cabinClass) {
        BigDecimal price = switch (cabinClass) {
            case ECONOMY, PREMIUM_ECONOMY -> flight.getBasePrice();
            case BUSINESS -> flight.getBusinessPrice() != null
                    ? flight.getBusinessPrice()
                    : flight.getBasePrice().multiply(new BigDecimal("2.5"));
            case FIRST_CLASS -> flight.getFirstClassPrice() != null
                    ? flight.getFirstClassPrice()
                    : flight.getBasePrice().multiply(new BigDecimal("4.0"));
        };

        // ✅ ЗАЩИТА ОТ NULL
        if (price == null) {
            log.warn("Base price is null for flight {}, class {}. Using default 100",
                    flight.getFlightNumber(), cabinClass);
            return new BigDecimal("100.00");
        }

        return price;
    }

    // ═══════════════════════════════════════
    // РАСЧЁТ ЗАГРУЖЕННОСТИ (С ЗАЩИТОЙ ОТ NULL)
    // ═══════════════════════════════════════

    /**
     * ✅ ЗАЩИТА ОТ NULL - рассчитать процент загруженности для класса
     */
    private int calculateOccupancyPercent(Flight flight, CabinClass cabinClass) {
        Integer totalSeats;
        Integer availableSeats;

        switch (cabinClass) {
            case ECONOMY, PREMIUM_ECONOMY -> {
                totalSeats = flight.getEconomySeats();
                availableSeats = flight.getAvailableEconomy();
            }
            case BUSINESS -> {
                totalSeats = flight.getBusinessSeats();
                availableSeats = flight.getAvailableBusiness();
            }
            case FIRST_CLASS -> {
                totalSeats = flight.getFirstClassSeats();
                availableSeats = flight.getAvailableFirstClass();
            }
            default -> {
                totalSeats = flight.getTotalSeats();
                availableSeats = flight.getAvailableSeats();
            }
        }

        // ✅ ЗАЩИТА ОТ NULL - если данные некорректные, считаем 0% загруженности
        if (totalSeats == null || totalSeats == 0 || availableSeats == null) {
            log.warn("Invalid seat data for flight {}, class {}: total={}, available={}",
                    flight.getFlightNumber(), cabinClass, totalSeats, availableSeats);
            return 0;
        }

        int occupiedSeats = totalSeats - availableSeats;

        // Защита от отрицательных значений
        if (occupiedSeats < 0) {
            log.warn("Negative occupied seats for flight {}, class {}: occupied={}",
                    flight.getFlightNumber(), cabinClass, occupiedSeats);
            return 0;
        }

        return (int) ((double) occupiedSeats / totalSeats * 100);
    }

    /**
     * ✅ ЗАЩИТА ОТ NULL - получить множитель на основе загруженности
     */
    private BigDecimal getOccupancyMultiplier(Flight flight, CabinClass cabinClass) {
        Integer totalSeats;
        Integer availableSeats;

        switch (cabinClass) {
            case ECONOMY, PREMIUM_ECONOMY -> {
                totalSeats = flight.getEconomySeats();
                availableSeats = flight.getAvailableEconomy();
            }
            case BUSINESS -> {
                totalSeats = flight.getBusinessSeats();
                availableSeats = flight.getAvailableBusiness();
            }
            case FIRST_CLASS -> {
                totalSeats = flight.getFirstClassSeats();
                availableSeats = flight.getAvailableFirstClass();
            }
            default -> {
                totalSeats = flight.getTotalSeats();
                availableSeats = flight.getAvailableSeats();
            }
        }

        // ✅ ЗАЩИТА ОТ NULL
        if (totalSeats == null || totalSeats == 0 || availableSeats == null) {
            log.warn("Invalid seat data for occupancy multiplier: flight {}, class {}",
                    flight.getFlightNumber(), cabinClass);
            return BigDecimal.ONE; // Базовая цена без множителя
        }

        int occupiedSeats = totalSeats - availableSeats;

        if (occupiedSeats < 0) {
            return BigDecimal.ONE;
        }

        double occupancyRate = (double) occupiedSeats / totalSeats;

        // Динамическое ценообразование на основе загруженности
        if (occupancyRate > 0.9) {
            return new BigDecimal("1.5");  // +50% при >90% загруженности
        } else if (occupancyRate > 0.75) {
            return new BigDecimal("1.3");  // +30% при >75%
        } else if (occupancyRate > 0.5) {
            return new BigDecimal("1.15"); // +15% при >50%
        } else {
            return BigDecimal.ONE;          // Базовая цена
        }
    }

    // ═══════════════════════════════════════
    // РАСЧЁТ ВРЕМЕНИ ДО ВЫЛЕТА
    // ═══════════════════════════════════════

    private long calculateDaysUntilDeparture(Flight flight, LocalDate bookingDate) {
        Instant departureInstant = flight.getDepartureTime();

        // ✅ ЗАЩИТА ОТ NULL
        if (departureInstant == null) {
            log.warn("Departure time is null for flight {}", flight.getFlightNumber());
            return 30; // Дефолтное значение
        }

        LocalDate departureDate = departureInstant
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        long days = Duration.between(
                bookingDate.atStartOfDay(),
                departureDate.atStartOfDay()
        ).toDays();

        return Math.max(0, days); // Не может быть отрицательным
    }

    /**
     * Множитель на основе времени до вылета
     */
    private BigDecimal getTimeMultiplier(long daysUntilDeparture) {
        if (daysUntilDeparture <= 1) {
            return new BigDecimal("1.3");      // +30% в последний день (было +100%)
        } else if (daysUntilDeparture <= 3) {
            return new BigDecimal("1.15");     // +15% за 3 дня (было +50%)
        } else if (daysUntilDeparture <= 7) {
            return new BigDecimal("1.1");      // +10% за неделю (было +30%)
        } else if (daysUntilDeparture <= 14) {
            return new BigDecimal("1.05");     // +5% за 2 недели (было +15%)
        } else {
            return BigDecimal.ONE;              // Нормальная цена
        }
    }

    // ═══════════════════════════════════════
    // ДЕНЬ НЕДЕЛИ
    // ═══════════════════════════════════════

    /**
     * Множитель на основе дня недели вылета
     */
    private BigDecimal getDayOfWeekMultiplier(Flight flight) {
        // ✅ ЗАЩИТА ОТ NULL
        if (flight.getDepartureTime() == null) {
            return BigDecimal.ONE;
        }

        int dayOfWeek = flight.getDepartureTime()
                .atZone(ZoneId.systemDefault())
                .getDayOfWeek()
                .getValue();

        // Пятница (5) и Воскресенье (7) - дороже
        if (dayOfWeek == 5 || dayOfWeek == 7) {
            return new BigDecimal("1.2");      // +20% в пик
        }
        // Среда (3) - дешевле
        else if (dayOfWeek == 3) {
            return new BigDecimal("0.9");      // -10% в середине недели
        }
        // Остальные дни - нормальная цена
        else {
            return BigDecimal.ONE;
        }
    }

    // ═══════════════════════════════════════
    // УРОВЕНЬ СПРОСА И РЕКОМЕНДАЦИИ
    // ═══════════════════════════════════════

    /**
     * Рассчитать уровень спроса на основе загруженности и времени до вылета
     */
    private DemandLevel calculateDemandLevel(int occupancyPercent, long daysUntilDeparture) {
        // Очень высокий спрос: >80% загруженность ИЛИ <3 дней до вылета
        if (occupancyPercent > 80 || daysUntilDeparture < 3) {
            return DemandLevel.VERY_HIGH;
        }
        // Высокий спрос: >60% загруженность ИЛИ <7 дней до вылета
        else if (occupancyPercent > 60 || daysUntilDeparture < 7) {
            return DemandLevel.HIGH;
        }
        // Средний спрос: >40% загруженность ИЛИ <14 дней до вылета
        else if (occupancyPercent > 40 || daysUntilDeparture < 14) {
            return DemandLevel.MEDIUM;
        }
        // Низкий спрос: всё остальное
        else {
            return DemandLevel.LOW;
        }
    }

    /**
     * Генерация рекомендации для пользователя
     */
    private String generateRecommendation(DemandLevel demandLevel, int occupancyPercent, long daysUntilDeparture) {
        return switch (demandLevel) {
            case VERY_HIGH -> {
                if (occupancyPercent > 90) {
                    yield "Only " + (100 - occupancyPercent) + "% seats left! Book now!";
                } else if (daysUntilDeparture <= 1) {
                    yield "Last minute booking - prices are high!";
                } else {
                    yield "High demand! Book now before sold out!";
                }
            }
            case HIGH -> {
                if (occupancyPercent > 70) {
                    yield "Good price but filling up fast - book soon!";
                } else {
                    yield "Popular flight - book within a week for best price";
                }
            }
            case MEDIUM -> {
                if (daysUntilDeparture > 30) {
                    yield "Great early bird price - excellent deal!";
                } else {
                    yield "Fair price - consider booking soon";
                }
            }
            case LOW -> {
                if (daysUntilDeparture > 60) {
                    yield "Best price! Book early and save up to 15%";
                } else {
                    yield "Low demand - good time to book at base price";
                }
            }
        };
    }

    // ═══════════════════════════════════════
    // ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ
    // ═══════════════════════════════════════

    /**
     * Получить доступные места для класса (с защитой от null)
     */
    @Override
    public int getAvailableSeatsForClass(Flight flight, CabinClass cabinClass) {
        Integer seats = switch (cabinClass) {
            case ECONOMY, PREMIUM_ECONOMY -> flight.getAvailableEconomy();
            case BUSINESS -> flight.getAvailableBusiness();
            case FIRST_CLASS -> flight.getAvailableFirstClass();
        };

        // ✅ ЗАЩИТА ОТ NULL
        if (seats == null) {
            log.warn("Available seats is null for flight {}, class {}",
                    flight.getFlightNumber(), cabinClass);
            return 0;
        }

        return seats;
    }

    /**
     * Проверить достаточность мест (с защитой от null)
     */
    @Override
    public boolean hasEnoughSeats(Flight flight, CabinClass cabinClass, int requiredSeats) {
        int available = getAvailableSeatsForClass(flight, cabinClass);
        return available >= requiredSeats;
    }

    /**
     * Рассчитать общую стоимость для нескольких пассажиров
     */
    @Override
    public BigDecimal calculateTotalPrice(
            Flight flight,
            CabinClass cabinClass,
            int passengerCount,
            LocalDate bookingDate
    ) {
        DynamicPriceResponse priceInfo = calculateDynamicPrice(flight, cabinClass, bookingDate);
        return priceInfo.getTotalPrice().multiply(new BigDecimal(passengerCount));
    }
}
