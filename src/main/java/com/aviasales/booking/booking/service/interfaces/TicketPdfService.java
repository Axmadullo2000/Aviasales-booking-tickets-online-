package com.aviasales.booking.booking.service.interfaces;

import com.aviasales.booking.booking.entity.Booking;
import com.aviasales.booking.booking.entity.Ticket;


/**
 * ✈️ UZBEKISTAN AIRWAYS BOARDING PASS GENERATOR
 * Генерирует посадочный талон в точности как реальный билет HY
 */
public interface TicketPdfService {

    // ═══════════════════════════════════════
    // ЦВЕТА UZBEKISTAN AIRWAYS (как на реальном билете)
    // ═══════════════════════════════════════
    /**
     * Генерирует PDF билет в стиле Uzbekistan Airways
     */
    byte[] generateTicketPdf(Booking booking);

    /**
     * Генерирует номер электронного билета
     */
    String generateTicketNumber(Booking booking, Ticket ticket);
}
