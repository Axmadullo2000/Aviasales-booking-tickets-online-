package com.aviasales.booking.booking.service.interfaces;

import com.aviasales.booking.booking.entity.Flight;
import com.aviasales.booking.booking.enums.CabinClass;
import com.aviasales.booking.booking.enums.SeatPreference;

import java.util.List;
import java.util.Set;


public interface SeatSelectionService {

    /**
     * Получить занятые места на рейсе
     */
    Set<String> getOccupiedSeats(Long flightId);

    /**
     * Проверить доступность места
     */
    boolean isSeatAvailable(Long flightId, String seatNumber);

    /**
     * Назначить место (запрошенное или автоматически)
     */
    String assignSeat(Flight flight, CabinClass cabinClass,
                             String requestedSeat, SeatPreference preference);

    /**
     * Автоматическое назначение места
     */
    String autoAssignSeat(Flight flight, CabinClass cabinClass, SeatPreference preference);

    /**
     * Генерация мест для класса
     */
    List<String> generateSeatsForClass(CabinClass cabinClass);

    /**
     * Проверка что место соответствует классу
     */
    boolean validateSeatForCabinClass(String seat, CabinClass cabinClass);

    /**
     * Фильтр мест по предпочтению
     */
    List<String> filterByPreference(List<String> seats, SeatPreference preference);

    /**
     * Проверка соответствия места предпочтению
     */
    boolean matchesPreference(String seat, SeatPreference preference);
}
