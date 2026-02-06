package com.monolit.booking.booking.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.monolit.booking.booking.entity.*;
import com.monolit.booking.booking.exception.FlightNotFoundException;
import com.monolit.booking.booking.repo.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketPdfService {

    private final FlightRepository flightRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter LOCAL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // Uzbekistan Airways brand colors
    private static final Color UZ_AIRWAYS_BLUE = new Color(0, 51, 153);
    private static final Color UZ_AIRWAYS_DARK_BLUE = new Color(0, 36, 107);
    private static final Color UZ_AIRWAYS_GOLD = new Color(197, 163, 92);
    private static final Color UZ_AIRWAYS_LIGHT_BLUE = new Color(230, 240, 255);
    private static final Color BORDER_COLOR = new Color(200, 200, 200);

    public byte[] generateTicketPdf(Booking booking) {
        log.info("Generating Uzbekistan Airways style PDF ticket for booking: {}", booking.getBookingReference());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // Add header with Uzbekistan Airways branding
            addUzAirwaysHeader(document, writer, booking);

            // Add flight information for each booking flight
            for (BookingFlight bookingFlight : booking.getBookingFlights()) {
                Flight flight = flightRepository.findById(bookingFlight.getFlightId())
                        .orElseThrow(() -> new FlightNotFoundException(bookingFlight.getFlightId()));
                addFlightCard(document, writer, flight, bookingFlight, booking);
            }

            // Add passenger details
            addPassengerSection(document, booking);

            // Add pricing section
            addPricingSection(document, booking);

            // Add boarding pass barcode
            addBoardingPassSection(document, booking, writer);

            // Add footer with important information
            addUzAirwaysFooter(document);

            document.close();
            log.info("PDF ticket generated successfully for booking: {}", booking.getBookingReference());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF ticket for booking: {}", booking.getBookingReference(), e);
            throw new RuntimeException("Failed to generate PDF ticket", e);
        }
    }

    private void addUzAirwaysHeader(Document document, PdfWriter writer, Booking booking) throws DocumentException {
        // Main header table
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2, 1});

        // Left cell - Company branding
        PdfPCell brandCell = new PdfPCell();
        brandCell.setBorder(Rectangle.NO_BORDER);
        brandCell.setPaddingBottom(15);

        Font companyFont = new Font(Font.HELVETICA, 26, Font.BOLD, UZ_AIRWAYS_BLUE);
        Font uzbekFont = new Font(Font.HELVETICA, 12, Font.NORMAL, Color.GRAY);
        Font taglineFont = new Font(Font.HELVETICA, 10, Font.ITALIC, UZ_AIRWAYS_GOLD);

        Paragraph companyName = new Paragraph("UZBEKISTAN AIRWAYS", companyFont);
        Paragraph uzbekName = new Paragraph("O'ZBEKISTON HAVO YO'LLARI", uzbekFont);
        Paragraph tagline = new Paragraph("National Air Carrier Since 1992", taglineFont);

        brandCell.addElement(companyName);
        brandCell.addElement(uzbekName);
        brandCell.addElement(tagline);
        headerTable.addCell(brandCell);

        // Right cell - E-Ticket info
        PdfPCell ticketInfoCell = new PdfPCell();
        ticketInfoCell.setBorder(Rectangle.NO_BORDER);
        ticketInfoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        ticketInfoCell.setPaddingBottom(15);

        Font eticketFont = new Font(Font.HELVETICA, 20, Font.BOLD, UZ_AIRWAYS_GOLD);
        Font refLabelFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);
        Font refValueFont = new Font(Font.HELVETICA, 14, Font.BOLD, UZ_AIRWAYS_BLUE);

        Paragraph eticketLabel = new Paragraph("E-TICKET", eticketFont);
        eticketLabel.setAlignment(Element.ALIGN_RIGHT);

        Paragraph refLabel = new Paragraph("Booking Reference", refLabelFont);
        refLabel.setAlignment(Element.ALIGN_RIGHT);

        Paragraph refValue = new Paragraph(booking.getBookingReference(), refValueFont);
        refValue.setAlignment(Element.ALIGN_RIGHT);

        ticketInfoCell.addElement(eticketLabel);
        ticketInfoCell.addElement(refLabel);
        ticketInfoCell.addElement(refValue);
        headerTable.addCell(ticketInfoCell);

        document.add(headerTable);

        // Gold separator line
        PdfPTable separator = new PdfPTable(1);
        separator.setWidthPercentage(100);
        PdfPCell sepCell = new PdfPCell();
        sepCell.setBorder(Rectangle.BOTTOM);
        sepCell.setBorderColor(UZ_AIRWAYS_GOLD);
        sepCell.setBorderWidth(3);
        sepCell.setFixedHeight(5);
        separator.addCell(sepCell);
        document.add(separator);

        // Status bar
        PdfPTable statusBar = new PdfPTable(2);
        statusBar.setWidthPercentage(100);
        statusBar.setSpacingBefore(10);

        Font statusLabelFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);
        Font statusValueFont = new Font(Font.HELVETICA, 10, Font.BOLD);

        PdfPCell statusCell = new PdfPCell();
        statusCell.setBorder(Rectangle.NO_BORDER);
        Paragraph statusLabel = new Paragraph("Ticket Status", statusLabelFont);
        Color statusColor = booking.getStatus().name().equals("CONFIRMED") ? new Color(0, 128, 0) : Color.ORANGE;
        Paragraph statusValue = new Paragraph(booking.getStatus().name(), new Font(Font.HELVETICA, 10, Font.BOLD, statusColor));
        statusCell.addElement(statusLabel);
        statusCell.addElement(statusValue);
        statusBar.addCell(statusCell);

        PdfPCell dateCell = new PdfPCell();
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph dateLabel = new Paragraph("Issue Date", statusLabelFont);
        dateLabel.setAlignment(Element.ALIGN_RIGHT);
        Paragraph dateValue = new Paragraph(DATE_FORMAT.format(booking.getCreatedAt()), statusValueFont);
        dateValue.setAlignment(Element.ALIGN_RIGHT);
        dateCell.addElement(dateLabel);
        dateCell.addElement(dateValue);
        statusBar.addCell(dateCell);

        document.add(statusBar);
    }

    private void addFlightCard(Document document, PdfWriter writer, Flight flight, BookingFlight bookingFlight, Booking booking) throws DocumentException {
        // Flight card container
        PdfPTable flightCard = new PdfPTable(1);
        flightCard.setWidthPercentage(100);
        flightCard.setSpacingBefore(20);

        PdfPCell cardContainer = new PdfPCell();
        cardContainer.setBorderColor(UZ_AIRWAYS_BLUE);
        cardContainer.setBorderWidth(2);
        cardContainer.setPadding(0);

        // Card header with flight number
        PdfPTable cardHeader = new PdfPTable(3);
        cardHeader.setWidthPercentage(100);
        cardHeader.setWidths(new float[]{1, 2, 1});

        PdfPCell flightNumCell = new PdfPCell();
        flightNumCell.setBackgroundColor(UZ_AIRWAYS_BLUE);
        flightNumCell.setPadding(10);
        flightNumCell.setBorder(Rectangle.NO_BORDER);
        Font flightNumFont = new Font(Font.HELVETICA, 14, Font.BOLD, Color.WHITE);
        Paragraph flightNum = new Paragraph(flight.getFlightNumber(), flightNumFont);
        flightNumCell.addElement(flightNum);
        cardHeader.addCell(flightNumCell);

        PdfPCell airlineCell = new PdfPCell();
        airlineCell.setBackgroundColor(UZ_AIRWAYS_BLUE);
        airlineCell.setPadding(10);
        airlineCell.setBorder(Rectangle.NO_BORDER);
        airlineCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Font airlineFont = new Font(Font.HELVETICA, 12, Font.NORMAL, Color.WHITE);
        Paragraph airlineName = new Paragraph(flight.getAirline().getName(), airlineFont);
        airlineName.setAlignment(Element.ALIGN_CENTER);
        airlineCell.addElement(airlineName);
        cardHeader.addCell(airlineCell);

        PdfPCell classCell = new PdfPCell();
        classCell.setBackgroundColor(UZ_AIRWAYS_GOLD);
        classCell.setPadding(10);
        classCell.setBorder(Rectangle.NO_BORDER);
        classCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Font classFont = new Font(Font.HELVETICA, 12, Font.BOLD, UZ_AIRWAYS_DARK_BLUE);
        Paragraph seatClass = new Paragraph(bookingFlight.getSeatClass().name(), classFont);
        seatClass.setAlignment(Element.ALIGN_CENTER);
        classCell.addElement(seatClass);
        cardHeader.addCell(classCell);

        // Route section
        PdfPTable routeTable = new PdfPTable(3);
        routeTable.setWidthPercentage(100);
        routeTable.setWidths(new float[]{2, 1, 2});

        // Departure
        PdfPCell depCell = createRouteCell(
                "DEPARTURE",
                flight.getDepartureAirport().getCity(),
                flight.getDepartureAirport().getIataCode(),
                flight.getDepartureAirport().getName(),
                DATE_FORMAT.format(flight.getDepartureTime()),
                TIME_FORMAT.format(flight.getDepartureTime()),
                Element.ALIGN_LEFT
        );
        routeTable.addCell(depCell);

        // Arrow/Duration
        PdfPCell arrowCell = new PdfPCell();
        arrowCell.setBorder(Rectangle.NO_BORDER);
        arrowCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        arrowCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        arrowCell.setPadding(15);
        arrowCell.setBackgroundColor(UZ_AIRWAYS_LIGHT_BLUE);

        int hours = flight.getDurationMinutes() / 60;
        int minutes = flight.getDurationMinutes() % 60;

        Font arrowFont = new Font(Font.HELVETICA, 24, Font.BOLD, UZ_AIRWAYS_BLUE);
        Font durationFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);

        Paragraph arrow = new Paragraph("✈", arrowFont);
        arrow.setAlignment(Element.ALIGN_CENTER);

        Paragraph duration = new Paragraph(hours + "h " + minutes + "m", durationFont);
        duration.setAlignment(Element.ALIGN_CENTER);

        arrowCell.addElement(arrow);
        arrowCell.addElement(duration);
        routeTable.addCell(arrowCell);

        // Arrival
        PdfPCell arrCell = createRouteCell(
                "ARRIVAL",
                flight.getArrivalAirport().getCity(),
                flight.getArrivalAirport().getIataCode(),
                flight.getArrivalAirport().getName(),
                DATE_FORMAT.format(flight.getArrivalTime()),
                TIME_FORMAT.format(flight.getArrivalTime()),
                Element.ALIGN_RIGHT
        );
        routeTable.addCell(arrCell);

        // Combine into card
        PdfPTable cardContent = new PdfPTable(1);
        cardContent.setWidthPercentage(100);

        PdfPCell headerCell = new PdfPCell(cardHeader);
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerCell.setPadding(0);
        cardContent.addCell(headerCell);

        PdfPCell routeCell = new PdfPCell(routeTable);
        routeCell.setBorder(Rectangle.NO_BORDER);
        routeCell.setPadding(0);
        cardContent.addCell(routeCell);

        cardContainer.addElement(cardContent);
        flightCard.addCell(cardContainer);
        document.add(flightCard);
    }

    private PdfPCell createRouteCell(String label, String city, String code, String airport, String date, String time, int alignment) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(15);
        cell.setBackgroundColor(UZ_AIRWAYS_LIGHT_BLUE);

        Font labelFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.GRAY);
        Font cityFont = new Font(Font.HELVETICA, 16, Font.BOLD, UZ_AIRWAYS_DARK_BLUE);
        Font codeFont = new Font(Font.HELVETICA, 28, Font.BOLD, UZ_AIRWAYS_BLUE);
        Font airportFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);
        Font dateFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
        Font timeFont = new Font(Font.HELVETICA, 22, Font.BOLD, UZ_AIRWAYS_DARK_BLUE);

        Paragraph pLabel = new Paragraph(label, labelFont);
        pLabel.setAlignment(alignment);

        Paragraph pCity = new Paragraph(city.toUpperCase(), cityFont);
        pCity.setAlignment(alignment);

        Paragraph pCode = new Paragraph(code, codeFont);
        pCode.setAlignment(alignment);

        Paragraph pAirport = new Paragraph(airport, airportFont);
        pAirport.setAlignment(alignment);

        Paragraph pDate = new Paragraph(date, dateFont);
        pDate.setAlignment(alignment);

        Paragraph pTime = new Paragraph(time, timeFont);
        pTime.setAlignment(alignment);

        cell.addElement(pLabel);
        cell.addElement(pCity);
        cell.addElement(pCode);
        cell.addElement(pAirport);
        cell.addElement(pDate);
        cell.addElement(pTime);

        return cell;
    }

    private void addPassengerSection(Document document, Booking booking) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD, UZ_AIRWAYS_BLUE);
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

        Paragraph passengerHeader = new Paragraph("PASSENGER INFORMATION", sectionFont);
        passengerHeader.setSpacingBefore(25);
        document.add(passengerHeader);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setWidths(new float[]{2.5f, 2f, 1.5f, 1.5f, 1f});

        // Table header
        String[] headers = {"PASSENGER NAME", "PASSPORT NO.", "DATE OF BIRTH", "NATIONALITY", "SEAT"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(UZ_AIRWAYS_BLUE);
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Table rows
        boolean alternate = false;
        for (Passenger passenger : booking.getPassengers()) {
            Color rowColor = alternate ? UZ_AIRWAYS_LIGHT_BLUE : Color.WHITE;
            alternate = !alternate;

            addPassengerCell(table, passenger.getFirstName() + " " + passenger.getLastName(), normalFont, rowColor);
            addPassengerCell(table, passenger.getPassportNumber(), normalFont, rowColor);
            addPassengerCell(table, passenger.getDateOfBirth().format(LOCAL_DATE_FORMAT), normalFont, rowColor);
            addPassengerCell(table, passenger.getNationality(), normalFont, rowColor);
            addPassengerCell(table, passenger.getSeatNumber() != null ? passenger.getSeatNumber() : "TBA", normalFont, rowColor);
        }

        document.add(table);
    }

    private void addPassengerCell(PdfPTable table, String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addPricingSection(Document document, Booking booking) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD, UZ_AIRWAYS_BLUE);
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font priceFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font totalLabelFont = new Font(Font.HELVETICA, 12, Font.BOLD, UZ_AIRWAYS_BLUE);
        Font totalValueFont = new Font(Font.HELVETICA, 14, Font.BOLD, UZ_AIRWAYS_GOLD);

        Paragraph pricingHeader = new Paragraph("FARE DETAILS", sectionFont);
        pricingHeader.setSpacingBefore(25);
        document.add(pricingHeader);

        // Pricing table
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setWidths(new float[]{3f, 1.5f, 1.5f, 2f});

        // Table header
        String[] headers = {"FLIGHT", "CLASS", "PASSENGERS", "PRICE"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(UZ_AIRWAYS_BLUE);
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Flight prices
        boolean alternate = false;
        for (BookingFlight bookingFlight : booking.getBookingFlights()) {
            Flight flight = flightRepository.findById(bookingFlight.getFlightId()).orElse(null);
            Color rowColor = alternate ? UZ_AIRWAYS_LIGHT_BLUE : Color.WHITE;
            alternate = !alternate;

            String flightInfo = flight != null
                    ? flight.getDepartureAirport().getIataCode() + " → " + flight.getArrivalAirport().getIataCode()
                    : "N/A";

            addPriceCell(table, flightInfo, normalFont, rowColor, Element.ALIGN_LEFT);
            addPriceCell(table, bookingFlight.getSeatClass().name(), normalFont, rowColor, Element.ALIGN_CENTER);
            addPriceCell(table, String.valueOf(bookingFlight.getPassengerCount()), normalFont, rowColor, Element.ALIGN_CENTER);
            addPriceCell(table, formatCurrency(bookingFlight.getTotalPrice()), priceFont, rowColor, Element.ALIGN_RIGHT);
        }

        document.add(table);

        // Total price section
        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(50);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.setSpacingBefore(15);

        // Separator line
        PdfPCell sepCell1 = new PdfPCell();
        sepCell1.setBorder(Rectangle.BOTTOM);
        sepCell1.setBorderColor(BORDER_COLOR);
        sepCell1.setFixedHeight(5);
        PdfPCell sepCell2 = new PdfPCell();
        sepCell2.setBorder(Rectangle.BOTTOM);
        sepCell2.setBorderColor(BORDER_COLOR);
        sepCell2.setFixedHeight(5);
        totalTable.addCell(sepCell1);
        totalTable.addCell(sepCell2);

        // Total row
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL FARE:", totalLabelFont));
        totalLabelCell.setBorder(Rectangle.NO_BORDER);
        totalLabelCell.setPaddingTop(10);
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        totalTable.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(new Phrase(formatCurrency(booking.getTotalPrice()), totalValueFont));
        totalValueCell.setBorder(Rectangle.NO_BORDER);
        totalValueCell.setPaddingTop(10);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.addCell(totalValueCell);

        document.add(totalTable);
    }

    private void addPriceCell(PdfPTable table, String text, Font font, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(8);
        cell.setHorizontalAlignment(alignment);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "N/A";
        return String.format("USD %.2f", amount);
    }

    private void addBoardingPassSection(Document document, Booking booking, PdfWriter writer) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD, UZ_AIRWAYS_BLUE);

        Paragraph barcodeSection = new Paragraph("BOARDING PASS BARCODE", sectionFont);
        barcodeSection.setAlignment(Element.ALIGN_CENTER);
        barcodeSection.setSpacingBefore(25);
        document.add(barcodeSection);

        try {
            Barcode128 barcode = new Barcode128();
            barcode.setCode(booking.getBookingReference());
            barcode.setCodeType(Barcode128.CODE128);
            Image barcodeImage = barcode.createImageWithBarcode(writer.getDirectContent(), UZ_AIRWAYS_DARK_BLUE, Color.WHITE);
            barcodeImage.setAlignment(Element.ALIGN_CENTER);
            barcodeImage.scalePercent(180);
            document.add(barcodeImage);

            Font barcodeTextFont = new Font(Font.HELVETICA, 12, Font.BOLD, UZ_AIRWAYS_BLUE);
            Paragraph barcodeText = new Paragraph(booking.getBookingReference(), barcodeTextFont);
            barcodeText.setAlignment(Element.ALIGN_CENTER);
            barcodeText.setSpacingBefore(5);
            document.add(barcodeText);

        } catch (Exception e) {
            log.warn("Could not generate barcode, using text placeholder", e);
            Font placeholderFont = new Font(Font.HELVETICA, 14, Font.BOLD, UZ_AIRWAYS_BLUE);
            Paragraph barcodePlaceholder = new Paragraph("[ " + booking.getBookingReference() + " ]", placeholderFont);
            barcodePlaceholder.setAlignment(Element.ALIGN_CENTER);
            document.add(barcodePlaceholder);
        }
    }

    private void addUzAirwaysFooter(Document document) throws DocumentException {
        // Separator
        PdfPTable separator = new PdfPTable(1);
        separator.setWidthPercentage(100);
        separator.setSpacingBefore(25);
        PdfPCell sepCell = new PdfPCell();
        sepCell.setBorder(Rectangle.BOTTOM);
        sepCell.setBorderColor(UZ_AIRWAYS_GOLD);
        sepCell.setBorderWidth(2);
        sepCell.setFixedHeight(1);
        separator.addCell(sepCell);
        document.add(separator);

        Font importantFont = new Font(Font.HELVETICA, 10, Font.BOLD, UZ_AIRWAYS_BLUE);
        Font normalFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        Font uzbekFont = new Font(Font.HELVETICA, 9, Font.ITALIC, Color.GRAY);

        Paragraph importantTitle = new Paragraph("IMPORTANT INFORMATION", importantFont);
        importantTitle.setSpacingBefore(15);
        document.add(importantTitle);

        Paragraph info1 = new Paragraph(
                "• Please arrive at the airport at least 2 hours before departure for domestic flights " +
                        "and 3 hours for international flights.",
                normalFont
        );
        info1.setSpacingBefore(5);
        document.add(info1);

        Paragraph info2 = new Paragraph(
                "• This is an electronic ticket. Please present a valid photo ID and this e-ticket at check-in.",
                normalFont
        );
        document.add(info2);

        Paragraph info3 = new Paragraph(
                "• Baggage allowance: Economy - 20kg, Business - 30kg. Excess baggage fees may apply.",
                normalFont
        );
        document.add(info3);

        Paragraph info4 = new Paragraph(
                "• For flight status and inquiries, call +998 99 749 42 62",
                normalFont
        );
        document.add(info4);

        // Thank you message
        Paragraph thankYou = new Paragraph(
                "\nThank you for choosing Uzbekistan Airways!\n" +
                        "Rahmat! O'zbekiston havo yo'llarini tanlaganingiz uchun!",
                new Font(Font.HELVETICA, 11, Font.BOLD, UZ_AIRWAYS_BLUE)
        );
        thankYou.setAlignment(Element.ALIGN_CENTER);
        thankYou.setSpacingBefore(15);
        document.add(thankYou);

        // Contact info
        Paragraph contact = new Paragraph(
                "41 Amir Temur Avenue, Tashkent 100060, Uzbekistan | Tel: +998 99 749 42 62 | www.uzairways.com",
                uzbekFont
        );
        contact.setAlignment(Element.ALIGN_CENTER);
        contact.setSpacingBefore(10);
        document.add(contact);
    }
}
