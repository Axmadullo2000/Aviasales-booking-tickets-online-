package com.aviasales.booking.booking.repo;

import com.aviasales.booking.booking.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AirportRepository extends JpaRepository<Airport, Long> {

    Optional<Airport> findByIataCode(String iataCode);

    List<Airport> findByIsActiveTrue();

    @Query("""
        SELECT a FROM Airport a
        WHERE a.isActive = true
        AND (
            LOWER(a.city) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(a.iataCode) LIKE LOWER(CONCAT('%', :query, '%'))
        )
    """)
    List<Airport> searchAirports(@Param("query") String query);
}
