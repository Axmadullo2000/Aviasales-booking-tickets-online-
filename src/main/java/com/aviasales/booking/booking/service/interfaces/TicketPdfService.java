package com.aviasales.booking.booking.service.interfaces;

import com.aviasales.booking.booking.entity.Booking;
import com.aviasales.booking.booking.entity.Ticket;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;


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
     * Генерирует один посадочный талон (как на фото)
     */
    void generateBoardingPass(Document document, PdfWriter writer, Ticket ticket, Booking booking)
            throws DocumentException;

    PdfPCell getPdfPCell();

    PdfPTable getPdfPTable();

    /**
     * Добавляет строку информации в правую часть билета
     */
    void addRightInfo(PdfPCell cell, String text, Font font);

    /**
     * Генерирует номер электронного билета
     */
    String generateTicketNumber(Booking booking, Ticket ticket);
}
