package com.aviasales.booking.booking.service.impl;

import com.aviasales.booking.booking.entity.Booking;
import com.aviasales.booking.booking.entity.Flight;
import com.aviasales.booking.booking.entity.Passenger;
import com.aviasales.booking.booking.entity.Ticket;
import com.aviasales.booking.booking.enums.CabinClass;
import com.aviasales.booking.booking.service.interfaces.TicketPdfService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketPdfServiceImpl implements TicketPdfService {

    private static final Color BG         = new Color(220, 240, 255);
    private static final Color UZ_BLUE    = new Color(0, 51, 153);
    private static final Color DIVIDER    = new Color(100, 149, 237);
    private static final Color GRAY_LABEL = new Color(90, 90, 90);

    private static final Color COLOR_ECONOMY  = new Color(30, 120, 60);
    private static final Color COLOR_BUSINESS = new Color(180, 120, 0);
    private static final Color COLOR_FIRST    = new Color(140, 0, 0);

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("ddMMMyyHHmm");

    // ─── Багаж ────────────────────────────────────────────────────────────────
    private int checkedKg(CabinClass c, Integer db) {
        if (db != null && db > 0) return db;
        return switch (c) {
            case ECONOMY -> 23;
            case BUSINESS -> 46;
            case FIRST_CLASS -> 64;
            default -> 0;
        };
    }

    private int handKg(CabinClass c, Integer db) {
        if (db != null && db > 0) return db;
        return switch (c) {
            case ECONOMY -> 8;
            case BUSINESS -> 15;
            case FIRST_CLASS -> 18;
            default -> 0;
        };
    }

    private String cabinCode(CabinClass c) {
        return switch (c) { case ECONOMY -> "Y"; case BUSINESS -> "C"; case FIRST_CLASS -> "F"; default -> ""; };
    }

    private String cabinName(CabinClass c) {
        return switch (c) {
            case ECONOMY -> "ECONOMY";
            case BUSINESS -> "BUSINESS";
            case FIRST_CLASS -> "FIRST CLASS";
            default -> "";
        };
    }

    private Color cabinColor(CabinClass c) {
        return switch (c) {
            case ECONOMY -> COLOR_ECONOMY;
            case BUSINESS -> COLOR_BUSINESS;
            case FIRST_CLASS -> COLOR_FIRST;
            default -> COLOR_BUSINESS; };
    }

    private PdfPCell bgCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(BG);
        return cell;
    }

    private void addRight(PdfPCell cell, String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_RIGHT);
        p.setSpacingAfter(1);
        cell.addElement(p);
    }

    // ─── Генерация PDF ────────────────────────────────────────────────────────
    @Override
    public byte[] generateTicketPdf(Booking booking) {
        log.info("Generating boarding pass for: {}", booking.getBookingReference());
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // КРИТИЧНО: используем A4 landscape — фиксированный размер,
            // каждый билет на отдельной странице. Это убирает infinite loop.
            Document doc = new Document(new Rectangle(842, 300), 0, 0, 0, 0);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();

            boolean first = true;
            for (Ticket ticket : booking.getTickets()) {
                if (!first) doc.newPage();
                generateBoardingPass(doc, writer, ticket, booking);
                first = false;
            }

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating boarding pass", e);
            throw new RuntimeException("Failed to generate boarding pass", e);
        }
    }

    // ─── Один талон ───────────────────────────────────────────────────────────
    public void generateBoardingPass(
            Document doc, PdfWriter writer,
            Ticket ticket, Booking booking
    ) throws DocumentException {

        Flight    flight    = ticket.getFlight();
        Passenger passenger = ticket.getPassenger();
        CabinClass cabin    = ticket.getCabinClass();

        int checked = checkedKg(cabin, ticket.getCheckedBaggage());
        int hand    = handKg(cabin, ticket.getHandLuggage());

        String passengerName = (passenger.getFirstName() + " " +
                passenger.getLastName()).toUpperCase();

        String eTicketNum = ticket.getTicketNumber() != null
                ? ticket.getTicketNumber()
                : generateTicketNumber(booking, ticket);

        ZonedDateTime dep = flight.getDepartureTimeLocal();
        String seatVal = ticket.getSeatNumber() != null ? ticket.getSeatNumber() : "—";
        String flightLine = flight.getFlightNumber() + "  " +
                dep.format(DT_FMT).toUpperCase() + "  " + cabinCode(cabin);

        // ── Фон на всю страницу через direct content ─────────────────────────
        // (таблица поверх, но ячейки тоже BG — двойная защита)
        PdfContentByte cb = writer.getDirectContentUnder();
        cb.saveState();
        cb.setColorFill(BG);
        cb.rectangle(0, 0, 842, 300);
        cb.fill();
        cb.restoreState();

        // ── Шрифты ────────────────────────────────────────────────────────────
        Font fLogoH   = new Font(Font.HELVETICA, 20, Font.BOLD,   UZ_BLUE);
        Font fLogoSub = new Font(Font.HELVETICA, 14, Font.NORMAL, UZ_BLUE);
        Font fLogoSm  = new Font(Font.HELVETICA,  9, Font.BOLD,   UZ_BLUE);
        Font fLogoXs  = new Font(Font.HELVETICA,  7, Font.NORMAL, UZ_BLUE);
        Font fName    = new Font(Font.HELVETICA, 13, Font.BOLD,   Color.BLACK);
        Font fRoute   = new Font(Font.HELVETICA, 17, Font.BOLD,   Color.BLACK);
        Font fFlight  = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(40, 40, 40));
        Font fCabin   = new Font(Font.HELVETICA, 11, Font.BOLD,   cabinColor(cabin));
        Font fLabel   = new Font(Font.HELVETICA,  7, Font.NORMAL, GRAY_LABEL);
        Font fBigKg   = new Font(Font.HELVETICA, 20, Font.BOLD,   Color.BLACK);
        Font fMidVal  = new Font(Font.HELVETICA, 16, Font.BOLD,   Color.BLACK);
        Font fEtkt    = new Font(Font.HELVETICA,  9, Font.NORMAL, new Color(60, 60, 60));
        Font fWish    = new Font(Font.HELVETICA,  8, Font.BOLD,   UZ_BLUE);
        Font fTiny    = new Font(Font.HELVETICA,  7, Font.NORMAL, Color.BLACK);
        Font fTinyL   = new Font(Font.HELVETICA,  7, Font.NORMAL, GRAY_LABEL);

        // ══════════════════════════════════════════════════════════════════════
        // ГЛАВНАЯ ТАБЛИЦА: 2 колонки — левая (контент) + правая (корешок)
        // ВАЖНО: NO вложенных таблиц — только параграфы! Это убирает loop.
        // ══════════════════════════════════════════════════════════════════════
        PdfPTable main = new PdfPTable(2);
        main.setTotalWidth(842);
        main.setLockedWidth(true);
        main.setWidths(new float[]{ 4.0f, 1.3f });
        main.setSplitRows(false);
        main.setKeepTogether(true);

        // ════════════════════════════════════
        // ЛЕВАЯ ЯЧЕЙКА
        // ════════════════════════════════════
        PdfPCell left = bgCell();
        left.setPaddingLeft(24);
        left.setPaddingTop(16);
        left.setPaddingBottom(14);
        left.setPaddingRight(10);
        // Фиксируем высоту — ключевой приём против infinite loop
        left.setMinimumHeight(280);

        // Логотип
        Paragraph pLogo = new Paragraph();
        pLogo.add(new Chunk("(HY)  ", new Font(Font.HELVETICA, 11, Font.BOLD, UZ_BLUE)));
        pLogo.add(new Chunk("UZBEKISTAN", fLogoH));
        pLogo.setSpacingAfter(1);
        left.addElement(pLogo);

        Paragraph pAirways = new Paragraph("airways", fLogoSub);
        pAirways.setSpacingAfter(10);
        left.addElement(pAirways);

        // Имя пассажира
        Paragraph pName = new Paragraph(passengerName, fName);
        pName.setSpacingAfter(6);
        left.addElement(pName);

        // Маршрут
        Paragraph pRoute = new Paragraph(
                flight.getOrigin().getIataCode() + "   →   " +
                        flight.getDestination().getIataCode(), fRoute);
        pRoute.setSpacingAfter(3);
        left.addElement(pRoute);

        // Рейс
        Paragraph pFlight = new Paragraph(flightLine, fFlight);
        pFlight.setSpacingAfter(4);
        left.addElement(pFlight);

        // Класс цветом
        Paragraph pCabin = new Paragraph(
                cabinCode(cabin) + "  •  " + cabinName(cabin), fCabin);
        pCabin.setSpacingAfter(14);
        left.addElement(pCabin);

        // ── Багаж и место — всё параграфами, NO вложенных таблиц ─────────────

        // Подписи в одну строку через Chunk с отступами
        Paragraph labelsRow = new Paragraph();
        labelsRow.add(new Chunk("ЗАРЕГ. БАГАЖ", fLabel));
        labelsRow.add(new Chunk("          ", fLabel)); // отступ
        labelsRow.add(new Chunk("    РУЧНАЯ КЛАДЬ", fLabel));
        labelsRow.add(new Chunk("          ", fLabel));
        labelsRow.add(new Chunk("    МЕСТО", fLabel));
        labelsRow.setSpacingAfter(1);
        left.addElement(labelsRow);

        // Значения в одну строку
        Paragraph valuesRow = new Paragraph();
        valuesRow.add(new Chunk(checked + " KG", fBigKg));
        valuesRow.add(new Chunk("         ", fBigKg));
        valuesRow.add(new Chunk("   " + hand + " KG", fMidVal));
        valuesRow.add(new Chunk("         ", fMidVal));
        valuesRow.add(new Chunk("   " + seatVal, fMidVal));
        valuesRow.setSpacingAfter(10);
        left.addElement(valuesRow);

        // E-TKT
        Paragraph pEtkt = new Paragraph("E-TKT  " + eTicketNum, fEtkt);
        pEtkt.setSpacingAfter(14);
        left.addElement(pEtkt);

        // Пожелания через Chunk — NO таблицы
        Paragraph pWishes = new Paragraph();
        pWishes.add(new Chunk("HAYRLI PARVOZ", fWish));
        pWishes.add(new Chunk("          HAVE A GOOD FLIGHT!          ", fWish));
        pWishes.add(new Chunk("СЧАСТЛИВОГО ПОЛЁТА!", fWish));
        left.addElement(pWishes);

        // ════════════════════════════════════
        // ПРАВАЯ ЯЧЕЙКА (корешок)
        // ════════════════════════════════════
        PdfPCell right = bgCell();
        right.setBorderWidthLeft(0.8f);
        right.setBorderColorLeft(DIVIDER);
        right.setBorderWidthRight(0f);
        right.setBorderWidthTop(0f);
        right.setBorderWidthBottom(0f);
        right.setPaddingLeft(12);
        right.setPaddingTop(16);
        right.setPaddingBottom(14);
        right.setPaddingRight(16);
        right.setMinimumHeight(280);

        // Лого справа
        Paragraph rLogo = new Paragraph("UZBEKISTAN", fLogoSm);
        rLogo.setAlignment(Element.ALIGN_RIGHT);
        rLogo.setSpacingAfter(1);
        right.addElement(rLogo);

        Paragraph rAirways = new Paragraph("airways", fLogoXs);
        rAirways.setAlignment(Element.ALIGN_RIGHT);
        rAirways.setSpacingAfter(10);
        right.addElement(rAirways);

        addRight(right, passengerName, fTiny);
        addRight(right, "", fTiny);

        Paragraph rCabin = new Paragraph(cabinCode(cabin) + " • " + cabinName(cabin), fCabin);
        rCabin.setAlignment(Element.ALIGN_RIGHT);
        rCabin.setSpacingAfter(5);
        right.addElement(rCabin);

        addRight(right, "ДОКУМЕНТ", fTinyL);
        addRight(right, passenger.getPassportNumber(), fTiny);
        addRight(right, "", fTiny);
        addRight(right, flight.getFlightNumber(), fTiny);
        addRight(right, dep.format(DT_FMT).toUpperCase(), fTiny);
        addRight(right, "", fTiny);
        addRight(right, "BAG: " + checked + " KG", fTiny);
        addRight(right, "CARRY: " + hand + " KG", fTiny);
        addRight(right, "", fTiny);
        addRight(right, "SEAT: " + seatVal, fTiny);
        addRight(right, "", fTiny);
        addRight(right, eTicketNum, fTiny);
        addRight(right, booking.getBookingReference(), fTiny);

        main.addCell(left);
        main.addCell(right);
        doc.add(main);
    }

    public String generateTicketNumber(Booking booking, Ticket ticket) {
        long ts  = System.currentTimeMillis() / 1000;
        String ref = booking.getBookingReference();
        String id  = ticket.getId() != null ? ticket.getId().toString() : "1";
        return String.format("250%d%sC%s", ts, ref, id);
    }
}
