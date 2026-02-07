package com.monolit.booking.booking.repo;

import com.monolit.booking.booking.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TicketRepository extends JpaRepository<Ticket, Long> {

}
