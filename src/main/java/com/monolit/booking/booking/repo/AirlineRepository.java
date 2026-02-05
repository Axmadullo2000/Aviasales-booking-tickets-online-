package com.monolit.booking.booking.repo;

import com.monolit.booking.booking.entity.Airline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AirlineRepository extends JpaRepository<Airline, Long> {

    Optional<Airline> findByIataCode(String iataCode);

    List<Airline> findByIsActiveTrue();

    boolean existsByIataCode(String iataCode);
}
