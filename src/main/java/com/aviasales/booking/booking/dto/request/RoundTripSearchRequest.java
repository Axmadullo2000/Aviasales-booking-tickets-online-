package com.aviasales.booking.booking.dto.request;

import com.aviasales.booking.booking.enums.CabinClass;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

/**
 * Запрос поиска билетов туда-обратно
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoundTripSearchRequest {

    /**
     * Откуда (IATA код аэропорта)
     */
    @NotBlank(message = "Origin airport is required")
    @Size(min = 3, max = 3, message = "Airport code must be 3 characters")
    private String from;

    /**
     * Куда (IATA код аэропорта)
     */
    @NotBlank(message = "Destination airport is required")
    @Size(min = 3, max = 3, message = "Airport code must be 3 characters")
    private String to;

    /**
     * Дата вылета туда
     */
    @NotNull(message = "Departure date is required")
    @FutureOrPresent(message = "Departure date must be today or in the future")
    private LocalDate departureDate;

    /**
     * Дата вылета обратно
     */
    @NotNull(message = "Return date is required")
    @Future(message = "Return date must be in the future")
    private LocalDate returnDate;

    /**
     * Количество пассажиров
     */
    @Min(value = 1, message = "At least 1 passenger required")
    @Max(value = 9, message = "Maximum 9 passengers allowed")
    private Integer passengers = 1;

    /**
     * Класс обслуживания
     */
    private CabinClass cabinClass = CabinClass.ECONOMY;

    /**
     * Гибкие даты (±3 дня)
     */
    private Boolean flexibleDates = false;

    /**
     * Только прямые рейсы
     */
    private Boolean directOnly = false;

    /**
     * Сортировка: PRICE, DURATION, DEPARTURE_TIME
     */
    private String sortBy = "PRICE";

    @AssertTrue(message = "Return date must be after departure date")
    public boolean isReturnAfterDeparture() {
        if (departureDate == null || returnDate == null) {
            return true; // Валидация на null будет в других аннотациях
        }
        return returnDate.isAfter(departureDate);
    }
}
