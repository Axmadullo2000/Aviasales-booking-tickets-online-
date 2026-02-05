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
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketPdfService {

    private final FlightRepository flightRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    public byte[] generateTicketPdf(Booking booking) {
        log.info("Generating PDF ticket for booking: {}", booking.getBookingReference());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            addHeader(document, booking);
            document.add(new Paragraph("\n"));

            for (BookingFlight bookingFlight : booking.getBookingFlights()) {
                Flight flight = flightRepository.findById(bookingFlight.getFlightId())
                        .orElseThrow(() -> new FlightNotFoundException(bookingFlight.getFlightId()));
                addFlightDetails(document, flight, bookingFlight);
                document.add(new Paragraph("\n"));
            }

            addPassengerDetails(document, booking);
            document.add(new Paragraph("\n"));

            addBarcodePlaceholder(document, booking, writer);

            addFooter(document);

            document.close();
            log.info("PDF ticket generated successfully for booking: {}", booking.getBookingReference());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF ticket for booking: {}", booking.getBookingReference(), e);
            throw new RuntimeException("Failed to generate PDF ticket", e);
        }
    }

    private void addHeader(Document document, Booking booking) throws DocumentException {
        Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, new Color(0, 51, 102));
        Font subtitleFont = new Font(Font.HELVETICA, 14, Font.NORMAL, Color.GRAY);

        Paragraph title = new Paragraph("E-TICKET", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph bookingRef = new Paragraph("Booking Reference: " + booking.getBookingReference(), subtitleFont);
        bookingRef.setAlignment(Element.ALIGN_CENTER);
        document.add(bookingRef);

        Paragraph status = new Paragraph("Status: " + booking.getStatus().name(), subtitleFont);
        status.setAlignment(Element.ALIGN_CENTER);
        document.add(status);
    }

    private void addFlightDetails(Document document, Flight flight, BookingFlight bookingFlight) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(0, 51, 102));
        Font normalFont = new Font(Font.HELVETICA, 11, Font.NORMAL);
        Font boldFont = new Font(Font.HELVETICA, 11, Font.BOLD);

        Paragraph flightHeader = new Paragraph("FLIGHT DETAILS", sectionFont);
        flightHeader.setSpacingBefore(10);
        document.add(flightHeader);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        addTableRow(table, "Flight Number:", flight.getFlightNumber(), boldFont, normalFont);
        addTableRow(table, "Airline:", flight.getAirline().getName(), boldFont, normalFont);
        addTableRow(table, "Class:", bookingFlight.getSeatClass().name(), boldFont, normalFont);

        document.add(table);

        PdfPTable routeTable = new PdfPTable(3);
        routeTable.setWidthPercentage(100);
        routeTable.setSpacingBefore(10);

        PdfPCell departureCell = new PdfPCell();
        departureCell.setBorder(Rectangle.NO_BORDER);
        departureCell.addElement(new Paragraph("DEPARTURE", new Font(Font.HELVETICA, 10, Font.BOLD, Color.GRAY)));
        departureCell.addElement(new Paragraph(flight.getDepartureAirport().getCity(), boldFont));
        departureCell.addElement(new Paragraph(flight.getDepartureAirport().getIataCode() + " - " + flight.getDepartureAirport().getName(), normalFont));
        departureCell.addElement(new Paragraph(flight.getDepartureTime().format(DATE_FORMAT), normalFont));
        departureCell.addElement(new Paragraph(flight.getDepartureTime().format(TIME_FORMAT), new Font(Font.HELVETICA, 16, Font.BOLD)));

        PdfPCell arrowCell = new PdfPCell();
        arrowCell.setBorder(Rectangle.NO_BORDER);
        arrowCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        arrowCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        arrowCell.addElement(new Paragraph(">>>", new Font(Font.HELVETICA, 20, Font.BOLD, new Color(0, 102, 204))));

        PdfPCell arrivalCell = new PdfPCell();
        arrivalCell.setBorder(Rectangle.NO_BORDER);
        arrivalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        arrivalCell.addElement(new Paragraph("ARRIVAL", new Font(Font.HELVETICA, 10, Font.BOLD, Color.GRAY)));
        arrivalCell.addElement(new Paragraph(flight.getArrivalAirport().getCity(), boldFont));
        arrivalCell.addElement(new Paragraph(flight.getArrivalAirport().getIataCode() + " - " + flight.getArrivalAirport().getName(), normalFont));
        arrivalCell.addElement(new Paragraph(flight.getArrivalTime().format(DATE_FORMAT), normalFont));
        arrivalCell.addElement(new Paragraph(flight.getArrivalTime().format(TIME_FORMAT), new Font(Font.HELVETICA, 16, Font.BOLD)));

        routeTable.addCell(departureCell);
        routeTable.addCell(arrowCell);
        routeTable.addCell(arrivalCell);

        document.add(routeTable);

        int hours = flight.getDurationMinutes() / 60;
        int minutes = flight.getDurationMinutes() % 60;
        Paragraph duration = new Paragraph("Duration: " + hours + "h " + minutes + "m", normalFont);
        duration.setAlignment(Element.ALIGN_CENTER);
        document.add(duration);
    }

    private void addPassengerDetails(Document document, Booking booking) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(0, 51, 102));
        Font normalFont = new Font(Font.HELVETICA, 11, Font.NORMAL);
        Font boldFont = new Font(Font.HELVETICA, 11, Font.BOLD);

        Paragraph passengerHeader = new Paragraph("PASSENGER DETAILS", sectionFont);
        passengerHeader.setSpacingBefore(20);
        document.add(passengerHeader);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        PdfPCell nameHeader = new PdfPCell(new Phrase("Name", boldFont));
        PdfPCell passportHeader = new PdfPCell(new Phrase("Passport", boldFont));
        PdfPCell dobHeader = new PdfPCell(new Phrase("Date of Birth", boldFont));
        PdfPCell seatHeader = new PdfPCell(new Phrase("Seat", boldFont));

        nameHeader.setBackgroundColor(new Color(230, 230, 230));
        passportHeader.setBackgroundColor(new Color(230, 230, 230));
        dobHeader.setBackgroundColor(new Color(230, 230, 230));
        seatHeader.setBackgroundColor(new Color(230, 230, 230));

        table.addCell(nameHeader);
        table.addCell(passportHeader);
        table.addCell(dobHeader);
        table.addCell(seatHeader);

        for (Passenger passenger : booking.getPassengers()) {
            table.addCell(new Phrase(passenger.getFirstName() + " " + passenger.getLastName(), normalFont));
            table.addCell(new Phrase(passenger.getPassportNumber(), normalFont));
            table.addCell(new Phrase(passenger.getDateOfBirth().format(DATE_FORMAT), normalFont));
            table.addCell(new Phrase(passenger.getSeatNumber() != null ? passenger.getSeatNumber() : "TBA", normalFont));
        }

        document.add(table);
    }

    private void addBarcodePlaceholder(Document document, Booking booking, PdfWriter writer) throws DocumentException {
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

        Paragraph barcodeLabel = new Paragraph("BOARDING PASS BARCODE", new Font(Font.HELVETICA, 10, Font.BOLD, Color.GRAY));
        barcodeLabel.setAlignment(Element.ALIGN_CENTER);
        barcodeLabel.setSpacingBefore(20);
        document.add(barcodeLabel);

        try {
            Barcode128 barcode = new Barcode128();
            barcode.setCode(booking.getBookingReference());
            barcode.setCodeType(Barcode128.CODE128);
            Image barcodeImage = barcode.createImageWithBarcode(writer.getDirectContent(), Color.BLACK, Color.WHITE);
            barcodeImage.setAlignment(Element.ALIGN_CENTER);
            barcodeImage.scalePercent(150);
            document.add(barcodeImage);
        } catch (Exception e) {
            log.warn("Could not generate barcode, using text placeholder", e);
            Paragraph barcodePlaceholder = new Paragraph("[" + booking.getBookingReference() + "]", normalFont);
            barcodePlaceholder.setAlignment(Element.ALIGN_CENTER);
            document.add(barcodePlaceholder);
        }
    }

    private void addFooter(Document document) throws DocumentException {
        Font footerFont = new Font(Font.HELVETICA, 9, Font.ITALIC, Color.GRAY);

        Paragraph footer = new Paragraph("\n\nPlease arrive at the airport at least 2 hours before departure for domestic flights " +
                "and 3 hours for international flights.", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        Paragraph disclaimer = new Paragraph("This is an electronic ticket. Please present a valid photo ID at check-in.", footerFont);
        disclaimer.setAlignment(Element.ALIGN_CENTER);
        document.add(disclaimer);
    }

    private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(valueCell);
    }
}
