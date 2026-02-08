package com.aviasales.booking.booking.repo;

import com.aviasales.booking.booking.entity.Flight;
import com.aviasales.booking.booking.enums.FlightStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    // ═══════════════════════════════════════
    // ПОИСК ПО НОМЕРУ РЕЙСА
    // ═══════════════════════════════════════

    Optional<Flight> findByFlightNumber(String flightNumber);

    boolean existsByFlightNumber(String flightNumber);

    // ═══════════════════════════════════════
    // ПОИСК РЕЙСОВ (ОСНОВНОЙ)
    // ═══════════════════════════════════════

    /**
     * Поиск рейсов по маршруту и дате
     * ✅ Исправлено: origin/destination вместо departureAirport/arrivalAirport
     */
    @Query("""
        SELECT f FROM Flight f
        WHERE f.origin.iataCode = :originCode
        AND f.destination.iataCode = :destinationCode
        AND f.departureTime >= :startDate
        AND f.departureTime < :endDate
        AND f.availableSeats >= :passengers
        AND f.status = :status
    """)
    Page<Flight> searchFlights(
            @Param("originCode") String originCode,
            @Param("destinationCode") String destinationCode,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("passengers") Integer passengers,
            @Param("status") FlightStatus status,
            Pageable pageable
    );

    /**
     * Поиск рейсов без фильтра по статусу
     */
    @Query("""
        SELECT f FROM Flight f
        WHERE f.origin.iataCode = :originCode
        AND f.destination.iataCode = :destinationCode
        AND f.departureTime >= :startDate
        AND f.departureTime < :endDate
        AND f.availableSeats >= :passengers
    """)
    Page<Flight> searchFlightsAllStatuses(
            @Param("originCode") String originCode,
            @Param("destinationCode") String destinationCode,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("passengers") Integer passengers,
            Pageable pageable
    );

    // ═══════════════════════════════════════
    // ПОИСК ПО АЭРОПОРТАМ И ВРЕМЕНИ
    // ═══════════════════════════════════════

    /**
     * Рейсы вылетающие из аэропорта за период
     * ✅ Исправлено: origin вместо departureAirport
     */
    List<Flight> findByOriginIataCodeAndDepartureTimeBetween(
            String iataCode,
            Instant start,
            Instant end
    );

    /**
     * Рейсы прилетающие в аэропорт за период
     * ✅ Исправлено: destination вместо arrivalAirport
     */
    List<Flight> findByDestinationIataCodeAndArrivalTimeBetween(
            String iataCode,
            Instant start,
            Instant end
    );

    // ═══════════════════════════════════════
    // ПОПУЛЯРНЫЕ НАПРАВЛЕНИЯ
    // ═══════════════════════════════════════

    /**
     * Топ популярных направлений
     * ✅ Исправлено: destination вместо arrivalAirport
     */
    @Query("""
        SELECT f.destination, COUNT(f) as flightCount
        FROM Flight f
        WHERE f.status = 'SCHEDULED'
        GROUP BY f.destination
        ORDER BY flightCount DESC
    """)
    List<Object[]> findPopularDestinations(Pageable pageable);

    // ═══════════════════════════════════════
    // ПОИСК ПО СТАТУСУ
    // ═══════════════════════════════════════

    List<Flight> findByStatus(FlightStatus status);

    /**
     * Найти рейсы вылетающие скоро (для напоминаний)
     */
    @Query("""
        SELECT f FROM Flight f
        WHERE f.status = 'SCHEDULED'
        AND f.departureTime BETWEEN :now AND :threshold
    """)
    List<Flight> findDepartingSoon(
            @Param("now") Instant now,
            @Param("threshold") Instant threshold
    );

    /**
     * Поиск рейсов по маршруту и диапазону дат для календаря цен
     */
    @Query("""
        SELECT f FROM Flight f
        WHERE f.origin.iataCode = :originCode
        AND f.destination.iataCode = :destinationCode
        AND f.departureTime >= :startDate
        AND f.departureTime <= :endDate
        AND f.status = 'SCHEDULED'
    """)
    List<Flight> findByOriginIataCodeAndDestinationIataCodeAndDepartureTimeBetween(
            @Param("originCode") String originCode,
            @Param("destinationCode") String destinationCode,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    // ═══════════════════════════════════════
    // ПОИСК ДЛЯ ROUND-TRIP SERVICE
    // ═══════════════════════════════════════

    /**
     * Поиск рейсов по маршруту и диапазону дат (для гибкого поиска ±3 дня)
     */
    @Query("""
        SELECT f FROM Flight f
        WHERE f.origin.iataCode = :originCode
        AND f.destination.iataCode = :destinationCode
        AND f.departureTime >= :startDateTime
        AND f.departureTime <= :endDateTime
        AND f.status = 'SCHEDULED'
        ORDER BY f.departureTime ASC
    """)
    List<Flight> findFlightsByRouteAndDateRange(
            @Param("originCode") String originCode,
            @Param("destinationCode") String destinationCode,
            @Param("startDateTime") Instant startDateTime,
            @Param("endDateTime") Instant endDateTime
    );

    /**
     * Поиск рейсов по маршруту и конкретной дате (для точного поиска)
     */
    @Query("""
        SELECT f FROM Flight f
        WHERE f.origin.iataCode = :originCode
        AND f.destination.iataCode = :destinationCode
        AND f.departureTime >= :startOfDay
        AND f.departureTime <= :endOfDay
        AND f.status = 'SCHEDULED'
        ORDER BY f.departureTime ASC
    """)
    List<Flight> findFlightsByRouteAndDate(
            @Param("originCode") String originCode,
            @Param("destinationCode") String destinationCode,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay
    );

    boolean existsByDepartureTimeLessThanAndArrivalTimeGreaterThan(Instant arrivalTime, Instant departureTime);

    @Modifying
    @Transactional
    @Query(value = """
        DELETE FROM tickets
        USING flights
        WHERE tickets.flight_id = flights.id
          AND flights.id = :id
    """, nativeQuery = true)
    void deleteTicketsByFlightId(@Param("id") Long id);


}