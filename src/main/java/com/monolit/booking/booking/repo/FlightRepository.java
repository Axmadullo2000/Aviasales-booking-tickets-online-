package com.monolit.booking.booking.repo;

import com.monolit.booking.booking.entity.Airport;
import com.monolit.booking.booking.entity.Flight;
import com.monolit.booking.booking.enums.FlightStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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
}
