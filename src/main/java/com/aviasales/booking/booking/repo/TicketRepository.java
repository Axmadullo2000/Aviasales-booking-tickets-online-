package com.aviasales.booking.booking.repo;

import com.aviasales.booking.booking.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;


public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByFlightId(Long flightId);
}
