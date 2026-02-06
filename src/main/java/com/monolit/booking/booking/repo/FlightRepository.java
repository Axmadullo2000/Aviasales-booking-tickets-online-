package com.monolit.booking.booking.repo;

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

    Optional<Flight> findByFlightNumber(String flightNumber);

    @Query("SELECT f FROM Flight f WHERE " +
           "f.departureAirport.iataCode = :departureAirport AND " +
           "f.arrivalAirport.iataCode = :arrivalAirport AND " +
           "f.departureTime >= :startDate AND " +
           "f.departureTime < :endDate AND " +
           "f.availableSeats >= :passengers AND " +
           "f.status = :status")
    Page<Flight> searchFlights(
            @Param("departureAirport") String departureAirport,
            @Param("arrivalAirport") String arrivalAirport,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("passengers") Integer passengers,
            @Param("status") FlightStatus status,
            Pageable pageable);

    @Query("SELECT f FROM Flight f WHERE " +
           "f.departureAirport.iataCode = :departureAirport AND " +
           "f.arrivalAirport.iataCode = :arrivalAirport AND " +
           "f.departureTime >= :startDate AND " +
           "f.departureTime < :endDate AND " +
           "f.availableSeats >= :passengers")
    Page<Flight> searchFlightsAllStatuses(
            @Param("departureAirport") String departureAirport,
            @Param("arrivalAirport") String arrivalAirport,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("passengers") Integer passengers,
            Pageable pageable);

    List<Flight> findByDepartureAirportIataCodeAndDepartureTimeBetween(
            String iataCode, Instant start, Instant end);

    List<Flight> findByArrivalAirportIataCodeAndArrivalTimeBetween(
            String iataCode, Instant start, Instant end);

    @Query("SELECT f.arrivalAirport, COUNT(f) as flightCount FROM Flight f " +
           "WHERE f.status = 'SCHEDULED' " +
           "GROUP BY f.arrivalAirport " +
           "ORDER BY flightCount DESC")
    List<Object[]> findPopularDestinations(Pageable pageable);

    List<Flight> findByStatus(FlightStatus status);

    boolean existsByFlightNumber(String flightNumber);
}
