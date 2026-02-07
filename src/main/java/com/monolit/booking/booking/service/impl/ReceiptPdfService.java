package com.monolit.booking.booking.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.monolit.booking.booking.entity.Receipt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class ReceiptPdfService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault());

    // Uzbekistan Airways brand colors
    private static final Color UZ_AIRWAYS_BLUE = new Color(0, 51, 153);
    private static final Color UZ_AIRWAYS_GOLD = new Color(197, 163, 92);
    private static final Color LIGHT_GRAY = new Color(245, 245, 245);
    private static final Color BORDER_GRAY = new Color(200, 200, 200);

    public byte[] generateReceiptPdf(Receipt receipt) {
        log.info("Generating receipt PDF for receipt number: {}", receipt.getReceiptNumber());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            addReceiptHeader(document, writer);
            document.add(new Paragraph("\n"));

            addReceiptTitle(document, receipt);
            document.add(new Paragraph("\n"));

            addPaymentDetails(document, receipt);
            document.add(new Paragraph("\n"));

            addFlightInfo(document, receipt);
            document.add(new Paragraph("\n"));

            addPriceBreakdown(document, receipt);
            document.add(new Paragraph("\n"));

            addReceiptFooter(document, receipt);

            document.close();
            log.info("Receipt PDF generated successfully for: {}", receipt.getReceiptNumber());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating receipt PDF: {}", receipt.getReceiptNumber(), e);
            throw new RuntimeException("Failed to generate receipt PDF", e);
        }
    }

    private void addReceiptHeader(Document document, PdfWriter writer) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 1});

        // Company Logo and Name
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPaddingBottom(10);

        Font companyFont = new Font(Font.HELVETICA, 22, Font.BOLD, UZ_AIRWAYS_BLUE);
        Font subtitleFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);

        Paragraph companyName = new Paragraph("UZBEKISTAN AIRWAYS", companyFont);
        Paragraph companySubtitle = new Paragraph("O'ZBEKISTON HAVO YO'LLARI", subtitleFont);
        Paragraph nationalCarrier = new Paragraph("National Air Carrier", subtitleFont);

        logoCell.addElement(companyName);
        logoCell.addElement(companySubtitle);
        logoCell.addElement(nationalCarrier);
        headerTable.addCell(logoCell);

        // Receipt Info
        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        infoCell.setPaddingBottom(10);

        Font receiptLabelFont = new Font(Font.HELVETICA, 18, Font.BOLD, UZ_AIRWAYS_GOLD);
        Paragraph receiptLabel = new Paragraph("PAYMENT RECEIPT", receiptLabelFont);
        receiptLabel.setAlignment(Element.ALIGN_RIGHT);

        Font infoFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);
        Paragraph address = new Paragraph("41 Amir Temur Avenue, Tashkent 100060", infoFont);
        address.setAlignment(Element.ALIGN_RIGHT);
        Paragraph phone = new Paragraph("Tel: +998 99 749 4262", infoFont);
        phone.setAlignment(Element.ALIGN_RIGHT);
        Paragraph website = new Paragraph("www.uzairways.com", infoFont);
        website.setAlignment(Element.ALIGN_RIGHT);

        infoCell.addElement(receiptLabel);
        infoCell.addElement(address);
        infoCell.addElement(phone);
        infoCell.addElement(website);
        headerTable.addCell(infoCell);

        document.add(headerTable);

        // Separator line
        PdfPTable separator = new PdfPTable(1);
        separator.setWidthPercentage(100);
        PdfPCell separatorCell = new PdfPCell();
        separatorCell.setBorder(Rectangle.BOTTOM);
        separatorCell.setBorderColor(UZ_AIRWAYS_GOLD);
        separatorCell.setBorderWidth(2);
        separatorCell.setFixedHeight(5);
        separator.addCell(separatorCell);
        document.add(separator);
    }

    private void addReceiptTitle(Document document, Receipt receipt) throws DocumentException {
        PdfPTable titleTable = new PdfPTable(2);
        titleTable.setWidthPercentage(100);
        titleTable.setSpacingBefore(15);

        Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.GRAY);
        Font valueFont = new Font(Font.HELVETICA, 12, Font.BOLD, UZ_AIRWAYS_BLUE);

        // Receipt Number
        PdfPCell receiptCell = new PdfPCell();
        receiptCell.setBorder(Rectangle.NO_BORDER);
        receiptCell.addElement(new Paragraph("Receipt Number", labelFont));
        receiptCell.addElement(new Paragraph(receipt.getReceiptNumber(), valueFont));
        titleTable.addCell(receiptCell);

        // Date
        PdfPCell dateCell = new PdfPCell();
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph dateLabel = new Paragraph("Issue Date", labelFont);
        dateLabel.setAlignment(Element.ALIGN_RIGHT);
        Paragraph dateValue = new Paragraph(DATE_TIME_FORMAT.format(receipt.getCreatedAt()), valueFont);
        dateValue.setAlignment(Element.ALIGN_RIGHT);
        dateCell.addElement(dateLabel);
        dateCell.addElement(dateValue);
        titleTable.addCell(dateCell);

        document.add(titleTable);
    }

    private void addPaymentDetails(Document document, Receipt receipt) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD, UZ_AIRWAYS_BLUE);
        Font labelFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
        Font valueFont = new Font(Font.HELVETICA, 10, Font.BOLD);

        Paragraph sectionTitle = new Paragraph("PAYMENT INFORMATION", sectionFont);
        sectionTitle.setSpacingBefore(15);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setWidths(new float[]{1, 2});

        addDetailRow(table, "Transaction ID:", receipt.getTransactionId(), labelFont, valueFont);
        addDetailRow(table, "Booking Reference:", receipt.getBookingReference(), labelFont, valueFont);
        addDetailRow(table, "Payment Method:", formatPaymentMethod(receipt.getPaymentMethod()), labelFont, valueFont);

        if (receipt.getCardLastFour() != null && !receipt.getCardLastFour().equals("****")) {
            addDetailRow(table, "Card Number:", "**** **** **** " + receipt.getCardLastFour(), labelFont, valueFont);
        }

        addDetailRow(table, "Payment Date:", DATE_TIME_FORMAT.format(receipt.getPaymentDate()), labelFont, valueFont);
        addDetailRow(table, "Passenger:", receipt.getPassengerName(), labelFont, valueFont);

        document.add(table);
    }

    private void addFlightInfo(Document document, Receipt receipt) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD, UZ_AIRWAYS_BLUE);
        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

        Paragraph sectionTitle = new Paragraph("FLIGHT DETAILS", sectionFont);
        sectionTitle.setSpacingBefore(15);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(LIGHT_GRAY);
        cell.setBorderColor(BORDER_GRAY);
        cell.setPadding(10);

        String[] flightLines = receipt.getFlightDetails().split("\n");
        for (String line : flightLines) {
            cell.addElement(new Paragraph(line, valueFont));
        }

        table.addCell(cell);
        document.add(table);
    }

    private void addPriceBreakdown(Document document, Receipt receipt) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD, UZ_AIRWAYS_BLUE);
        Font labelFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font totalLabelFont = new Font(Font.HELVETICA, 12, Font.BOLD, UZ_AIRWAYS_BLUE);
        Font totalValueFont = new Font(Font.HELVETICA, 14, Font.BOLD, UZ_AIRWAYS_GOLD);

        Paragraph sectionTitle = new Paragraph("PAYMENT BREAKDOWN", sectionFont);
        sectionTitle.setSpacingBefore(15);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setSpacingBefore(10);
        table.setWidths(new float[]{2, 1});

        BigDecimal baseFare = receipt.getAmount();
        BigDecimal tax = baseFare.multiply(BigDecimal.valueOf(0.05)); // 5%
        BigDecimal total = baseFare.add(tax).add(receipt.getServiceFee() != null ? receipt.getServiceFee() : BigDecimal.ZERO);

        addPriceRow(table, "Base Fare:", formatCurrency(baseFare, receipt.getCurrency()), labelFont, valueFont);
        addPriceRow(table, "Tax (5%):", formatCurrency(tax, receipt.getCurrency()), labelFont, valueFont);

        if (receipt.getServiceFee() != null && receipt.getServiceFee().compareTo(BigDecimal.ZERO) > 0) {
            addPriceRow(table, "Service Fee:", formatCurrency(receipt.getServiceFee(), receipt.getCurrency()), labelFont, valueFont);
        }

        // Separator
        PdfPCell separatorCell1 = new PdfPCell();
        separatorCell1.setBorder(Rectangle.BOTTOM);
        separatorCell1.setBorderColor(BORDER_GRAY);
        separatorCell1.setFixedHeight(10);
        PdfPCell separatorCell2 = new PdfPCell();
        separatorCell2.setBorder(Rectangle.BOTTOM);
        separatorCell2.setBorderColor(BORDER_GRAY);
        separatorCell2.setFixedHeight(10);
        table.addCell(separatorCell1);
        table.addCell(separatorCell2);

        // Total
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL PAID:", totalLabelFont));
        totalLabelCell.setBorder(Rectangle.NO_BORDER);
        totalLabelCell.setPaddingTop(10);
        table.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(new Phrase(formatCurrency(total, receipt.getCurrency()), totalValueFont));
        totalValueCell.setBorder(Rectangle.NO_BORDER);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValueCell.setPaddingTop(10);
        table.addCell(totalValueCell);

        document.add(table);

        // Payment Status (как было)
        PdfPTable statusTable = new PdfPTable(1);
        statusTable.setWidthPercentage(100);
        statusTable.setSpacingBefore(20);

        PdfPCell statusCell = new PdfPCell();
        statusCell.setBackgroundColor(new Color(220, 255, 220));
        statusCell.setBorderColor(new Color(0, 150, 0));
        statusCell.setPadding(10);
        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Font statusFont = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(0, 100, 0));
        Paragraph statusText = new Paragraph("✓ PAYMENT SUCCESSFUL", statusFont);
        statusText.setAlignment(Element.ALIGN_CENTER);
        statusCell.addElement(statusText);

        statusTable.addCell(statusCell);
        document.add(statusTable);
    }


    private void addReceiptFooter(Document document, Receipt receipt) throws DocumentException {
        Font footerFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
        Font footerBoldFont = new Font(Font.HELVETICA, 8, Font.BOLD, Color.GRAY);

        Paragraph separator = new Paragraph("\n");
        separator.setSpacingBefore(30);
        document.add(separator);

        // Separator line
        PdfPTable separatorTable = new PdfPTable(1);
        separatorTable.setWidthPercentage(100);
        PdfPCell separatorCell = new PdfPCell();
        separatorCell.setBorder(Rectangle.BOTTOM);
        separatorCell.setBorderColor(BORDER_GRAY);
        separatorCell.setFixedHeight(1);
        separatorTable.addCell(separatorCell);
        document.add(separatorTable);

        Paragraph terms = new Paragraph("TERMS AND CONDITIONS", footerBoldFont);
        terms.setSpacingBefore(10);
        document.add(terms);

        Paragraph termsText = new Paragraph(
                "This receipt confirms your payment for the flight booking. " +
                        "Please retain this document for your records. " +
                        "For any inquiries regarding refunds or changes, please contact our customer service at +998 78 140 0101 " +
                        "or visit our website at www.uzairways.com. " +
                        "Refund policies are subject to fare rules and may incur additional fees.",
                footerFont
        );
        termsText.setSpacingBefore(5);
        document.add(termsText);

        Paragraph thankYou = new Paragraph(
                "\nThank you for choosing Uzbekistan Airways!\n" +
                        "Rahmat! O'zbekiston havo yo'llarini tanlaganingiz uchun!",
                new Font(Font.HELVETICA, 10, Font.ITALIC, UZ_AIRWAYS_BLUE)
        );
        thankYou.setAlignment(Element.ALIGN_CENTER);
        thankYou.setSpacingBefore(15);
        document.add(thankYou);
    }

    private void addDetailRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(5);
        table.addCell(valueCell);
    }

    private void addPriceRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(3);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPaddingBottom(3);
        table.addCell(valueCell);
    }

    private String formatPaymentMethod(String method) {
        if (method == null) return "N/A";
        return switch (method) {
            case "CREDIT_CARD" -> "Credit Card";
            case "DEBIT_CARD" -> "Debit Card";
            case "PAYPAL" -> "PayPal";
            case "BANK_TRANSFER" -> "Bank Transfer";
            default -> method;
        };
    }

    private String formatCurrency(BigDecimal amount, String currency) {
        return String.format("%s %.2f", currency, amount);
    }
}
