package com.monolit.booking.booking.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.monolit.booking.booking.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ✈️ UZBEKISTAN AIRWAYS BOARDING PASS GENERATOR
 * Генерирует посадочный талон в точности как реальный билет HY
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketPdfService {

    // ═══════════════════════════════════════
    // ЦВЕТА UZBEKISTAN AIRWAYS (как на реальном билете)
    // ═══════════════════════════════════════

    private static final Color UZ_BLUE = new Color(0, 51, 153);           // Тёмно-синий
    private static final Color UZ_LIGHT_BLUE = new Color(220, 240, 255);  // Светло-голубой фон
    private static final Color UZ_SKY_BLUE = new Color(135, 206, 250);    // Акценты

    // ═══════════════════════════════════════
    // ФОРМАТТЕРЫ
    // ═══════════════════════════════════════

    private static final DateTimeFormatter DATE_FLIGHT_FORMAT = DateTimeFormatter.ofPattern("ddMMMyyHHmm");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Генерирует PDF билет в стиле Uzbekistan Airways
     */
    public byte[] generateTicketPdf(Booking booking) {
        log.info("Generating Uzbekistan Airways boarding pass for: {}", booking.getBookingReference());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // ✅ Размер как у реального посадочного талона (21cm x 8cm)
            Rectangle boardingPassSize = new Rectangle(595, 227); // A4 width x custom height
            Document document = new Document(boardingPassSize, 20, 20, 15, 15);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // Генерируем билет для каждого пассажира
            boolean isFirst = true;
            for (Ticket ticket : booking.getTickets()) {
                if (!isFirst) {
                    document.newPage();
                }
                generateBoardingPass(document, writer, ticket, booking);
                isFirst = false;
            }

            document.close();

            byte[] pdfBytes = baos.toByteArray();
            log.info("Boarding pass generated, size: {} bytes ({} KB)",
                    pdfBytes.length, pdfBytes.length / 1024);

            return pdfBytes;

        } catch (Exception e) {
            log.error("Error generating boarding pass for: {}", booking.getBookingReference(), e);
            throw new RuntimeException("Failed to generate boarding pass", e);
        }
    }

    /**
     * Генерирует один посадочный талон (как на фото)
     */
    private void generateBoardingPass(Document document, PdfWriter writer, Ticket ticket, Booking booking)
            throws DocumentException {

        Flight flight = ticket.getFlight();
        Passenger passenger = ticket.getPassenger();

        // ✅ ФОН - светло-голубой (как на реальном билете)
        PdfContentByte canvas = writer.getDirectContentUnder();
        canvas.saveState();
        canvas.setColorFill(UZ_LIGHT_BLUE);
        canvas.rectangle(0, 0, 595, 227);
        canvas.fill();
        canvas.restoreState();

        // ═══════════════════════════════════════
        // ГЛАВНАЯ ТАБЛИЦА
        // ═══════════════════════════════════════

        PdfPTable mainTable = new PdfPTable(2);
        mainTable.setWidthPercentage(100);
        mainTable.setWidths(new float[]{3.5f, 1.5f}); // Левая часть шире

        // ═══════════════════════════════════════
        // ЛЕВАЯ ЧАСТЬ
        // ═══════════════════════════════════════

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(10);

        // Логотип и название
        Font logoFont = new Font(Font.HELVETICA, 20, Font.BOLD, UZ_BLUE);
        Font airwaysFont = new Font(Font.HELVETICA, 16, Font.NORMAL, UZ_BLUE);

        Paragraph logo = new Paragraph();
        logo.add(new Chunk("(HY) ", new Font(Font.HELVETICA, 12, Font.BOLD, UZ_BLUE)));
        logo.add(new Chunk("UZBEKISTAN", logoFont));
        logo.setSpacingAfter(2);
        leftCell.addElement(logo);

        Paragraph airways = new Paragraph("airways", airwaysFont);
        airways.setSpacingAfter(15);
        leftCell.addElement(airways);

        // Имя пассажира (БОЛЬШИМИ БУКВАМИ как на билете)
        Font nameFont = new Font(Font.HELVETICA, 14, Font.BOLD, Color.BLACK);
        String passengerName = (passenger.getFirstName() + " " + passenger.getLastName()).toUpperCase();
        Paragraph name = new Paragraph(passengerName, nameFont);
        name.setSpacingAfter(10);
        leftCell.addElement(name);

        // Маршрут (FROM → TO)
        Font routeFont = new Font(Font.HELVETICA, 12, Font.BOLD, Color.BLACK);
        Paragraph route = new Paragraph(
                flight.getOrigin().getIataCode() + " → " + flight.getDestination().getIataCode(),
                routeFont
        );
        route.setSpacingAfter(5);
        leftCell.addElement(route);

        // Номер рейса + класс + дата/время
        ZonedDateTime departure = flight.getDepartureTimeLocal();
        String flightInfo = String.format("%s %s %s",
                flight.getFlightNumber(),
                ticket.getCabinClass().toString().charAt(0), // Y/B/F
                departure.format(DATE_FLIGHT_FORMAT).toUpperCase()
        );
        Font flightFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);
        Paragraph flightDetails = new Paragraph(flightInfo, flightFont);
        flightDetails.setSpacingAfter(20);
        leftCell.addElement(flightDetails);

        // Багаж (крупно, как на фото)
        Font baggageFont = new Font(Font.HELVETICA, 24, Font.BOLD, Color.BLACK);
        Paragraph baggage = new Paragraph(
                ticket.getCheckedBaggage() != null ? ticket.getCheckedBaggage().toString() : "0",
                baggageFont
        );
        baggage.setSpacingAfter(15);
        leftCell.addElement(baggage);

        // E-TKT номер
        Font ticketNumFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
        String eTicketNumber = ticket.getTicketNumber() != null
                ? ticket.getTicketNumber()
                : generateTicketNumber(booking, ticket);
        Paragraph ticketNum = new Paragraph("E-TKT " + eTicketNumber, ticketNumFont);
        ticketNum.setSpacingAfter(15);
        leftCell.addElement(ticketNum);

        // Место (крупно)
        Font seatFont = new Font(Font.HELVETICA, 20, Font.BOLD, Color.BLACK);
        Paragraph seat = new Paragraph(
                ticket.getSeatNumber() != null ? ticket.getSeatNumber() : "—",
                seatFont
        );
        seat.setSpacingAfter(15);
        leftCell.addElement(seat);

        // Пожелания (как на реальном билете)
        Font wishFont = new Font(Font.HELVETICA, 9, Font.BOLD, UZ_BLUE);
        PdfPTable wishTable = new PdfPTable(3);
        wishTable.setWidthPercentage(100);

        PdfPCell wish1 = new PdfPCell(new Phrase("KAYTLI PARVOZ", wishFont));
        wish1.setBorder(Rectangle.NO_BORDER);
        wish1.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell wish2 = new PdfPCell(new Phrase("HAVE A GOOD FLIGHT!", wishFont));
        wish2.setBorder(Rectangle.NO_BORDER);
        wish2.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell wish3 = new PdfPCell(new Phrase("СЧАСТЛИВОГО ПОЛЁТА!", wishFont));
        wish3.setBorder(Rectangle.NO_BORDER);
        wish3.setHorizontalAlignment(Element.ALIGN_RIGHT);

        wishTable.addCell(wish1);
        wishTable.addCell(wish2);
        wishTable.addCell(wish3);

        leftCell.addElement(wishTable);

        mainTable.addCell(leftCell);

        // ═══════════════════════════════════════
        // ПРАВАЯ ЧАСТЬ (дублирование информации)
        // ═══════════════════════════════════════

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(10);
        rightCell.setVerticalAlignment(Element.ALIGN_TOP);

        // Маленький логотип сверху справа
        Font smallLogoFont = new Font(Font.HELVETICA, 10, Font.BOLD, UZ_BLUE);
        Paragraph smallLogo = new Paragraph("UZBEKISTAN", smallLogoFont);
        smallLogo.setAlignment(Element.ALIGN_RIGHT);
        smallLogo.setSpacingAfter(2);
        rightCell.addElement(smallLogo);

        Font smallAirwaysFont = new Font(Font.HELVETICA, 8, Font.NORMAL, UZ_BLUE);
        Paragraph smallAirways = new Paragraph("airways", smallAirwaysFont);
        smallAirways.setAlignment(Element.ALIGN_RIGHT);
        smallAirways.setSpacingAfter(10);
        rightCell.addElement(smallAirways);

        // Информация справа
        Font rightFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);

        addRightInfo(rightCell, passengerName, rightFont);
        addRightInfo(rightCell, "", rightFont); // пустая строка
        addRightInfo(rightCell, "ДОКУМЕНТ", rightFont);
        addRightInfo(rightCell, passenger.getPassportNumber(), rightFont);
        addRightInfo(rightCell, "", rightFont);
        addRightInfo(rightCell, flightInfo, rightFont);
        addRightInfo(rightCell, "", rightFont);
        addRightInfo(rightCell, ticket.getCheckedBaggage() + " KG", rightFont);
        addRightInfo(rightCell, "", rightFont);
        addRightInfo(rightCell, "3", rightFont);
        addRightInfo(rightCell, eTicketNumber, rightFont);

        mainTable.addCell(rightCell);

        document.add(mainTable);
    }

    /**
     * Добавляет строку информации в правую часть билета
     */
    private void addRightInfo(PdfPCell cell, String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_RIGHT);
        p.setSpacingAfter(2);
        cell.addElement(p);
    }

    /**
     * Генерирует номер электронного билета
     */
    private String generateTicketNumber(Booking booking, Ticket ticket) {
        // Формат: 2502433494261C2 (как на реальном билете)
        String bookingRef = booking.getBookingReference();
        String ticketId = ticket.getId() != null ? ticket.getId().toString() : "1";

        // Генерируем 13-значный номер + C + номер билета
        long timestamp = System.currentTimeMillis() / 1000;
        return String.format("250%d%sC%s", timestamp, bookingRef, ticketId);
    }
}
