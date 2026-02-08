package com.aviasales.booking.booking.repo;

import com.aviasales.booking.booking.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Set;


public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * Найти все билеты по ID рейса
     */
    List<Ticket> findByFlightId(Long flightId);

    /**
     * Подсчитать количество билетов для рейса
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.flight.id = :flightId")
    long countByFlightId(Long flightId);
}
