package com.aviasales.booking.booking.service.impl;

import com.aviasales.booking.booking.entity.Flight;
import com.aviasales.booking.booking.entity.Ticket;
import com.aviasales.booking.booking.enums.CabinClass;
import com.aviasales.booking.booking.enums.SeatPreference;
import com.aviasales.booking.booking.repo.TicketRepository;
import com.aviasales.booking.booking.service.interfaces.SeatSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class SeatSelectionServiceImpl implements SeatSelectionService {

    private final TicketRepository ticketRepository;

    /**
     * Получить занятые места на рейсе
     */
    public Set<String> getOccupiedSeats(Long flightId) {
        return ticketRepository.findByFlightId(flightId).stream()
                .map(Ticket::getSeatNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Проверить доступность места
     */
    public boolean isSeatAvailable(Long flightId, String seatNumber) {
        if (seatNumber == null) return true;

        Set<String> occupied = getOccupiedSeats(flightId);
        return !occupied.contains(seatNumber);
    }

    /**
     * Назначить место (запрошенное или автоматически)
     */
    public String assignSeat(Flight flight, CabinClass cabinClass,
                             String requestedSeat, SeatPreference preference) {

        // Если место указано явно
        if (requestedSeat != null && !requestedSeat.isBlank()) {
            if (!isSeatAvailable(flight.getId(), requestedSeat)) {
                throw new IllegalArgumentException(
                        "Seat " + requestedSeat + " is already taken"
                );
            }

            // Проверяем что место соответствует классу
            if (!validateSeatForCabinClass(requestedSeat, cabinClass)) {
                throw new IllegalArgumentException(
                        "Seat " + requestedSeat + " is not in " + cabinClass + " class"
                );
            }

            return requestedSeat;
        }

        // Автоматическое назначение на основе preference
        return autoAssignSeat(flight, cabinClass, preference);
    }

    /**
     * Автоматическое назначение места
     */
    public String autoAssignSeat(Flight flight, CabinClass cabinClass, SeatPreference preference) {
        Set<String> occupied = getOccupiedSeats(flight.getId());
        List<String> availableSeats = generateSeatsForClass(cabinClass);

        // Фильтруем занятые
        availableSeats = availableSeats.stream()
                .filter(seat -> !occupied.contains(seat))
                .collect(Collectors.toList());

        if (availableSeats.isEmpty()) {
            throw new IllegalStateException("No available seats in " + cabinClass);
        }

        // Фильтруем по предпочтению
        if (preference != null) {
            List<String> preferredSeats = filterByPreference(availableSeats, preference);
            if (!preferredSeats.isEmpty()) {
                return preferredSeats.get(0);
            }
        }

        // Возвращаем первое доступное
        return availableSeats.get(0);
    }

    /**
     * Генерация мест для класса
     */
    public List<String> generateSeatsForClass(CabinClass cabinClass) {
        List<String> seats = new ArrayList<>();

        switch (cabinClass) {
            case FIRST_CLASS -> {
                // Ряды 1-2, места A-D (4 места в ряду)
                for (int row = 1; row <= 2; row++) {
                    for (char letter = 'A'; letter <= 'D'; letter++) {
                        seats.add(row + String.valueOf(letter));
                    }
                }
            }
            case BUSINESS -> {
                // Ряды 3-8, места A-F (6 мест в ряду, 2-2-2)
                for (int row = 3; row <= 8; row++) {
                    for (char letter = 'A'; letter <= 'F'; letter++) {
                        seats.add(row + String.valueOf(letter));
                    }
                }
            }
            case ECONOMY, PREMIUM_ECONOMY -> {
                // Ряды 9-35, места A-F (6 мест в ряду, 3-3)
                for (int row = 9; row <= 35; row++) {
                    for (char letter = 'A'; letter <= 'F'; letter++) {
                        seats.add(row + String.valueOf(letter));
                    }
                }
            }
        }

        return seats;
    }

    /**
     * Проверка что место соответствует классу
     */
    public boolean validateSeatForCabinClass(String seat, CabinClass cabinClass) {
        return extractTypeCabin(seat, cabinClass);
    }


    static boolean extractTypeCabin(String seat, CabinClass cabinClass) {
        int row = Integer.parseInt(seat.replaceAll("[^0-9]", ""));

        return switch (cabinClass) {
            case FIRST_CLASS -> row >= 1 && row <= 2;
            case BUSINESS -> row >= 3 && row <= 8;
            case ECONOMY, PREMIUM_ECONOMY -> row >= 9 && row <= 35;
        };
    }
    /**
     * Фильтр мест по предпочтению
     */
    public List<String> filterByPreference(List<String> seats, SeatPreference preference) {
        return seats.stream()
                .filter(seat -> matchesPreference(seat, preference))
                .collect(Collectors.toList());
    }

    /**
     * Проверка соответствия места предпочтению
     */
    public boolean matchesPreference(String seat, SeatPreference preference) {
        char letter = seat.charAt(seat.length() - 1);

        return switch (preference) {
            case WINDOW -> letter == 'A' || letter == 'F';  // Окно
            case AISLE -> letter == 'C' || letter == 'D';   // Проход
            case MIDDLE -> letter == 'B' || letter == 'E';  // Середина
        };
    }
}
