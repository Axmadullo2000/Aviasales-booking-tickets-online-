package com.aviasales.booking.booking.service.impl;

import com.aviasales.booking.booking.entity.Receipt;
import com.aviasales.booking.booking.service.interfaces.ReceiptPdfService;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


@Slf4j
@Service
public class ReceiptPdfServiceImpl implements ReceiptPdfService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter
            .ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("dd MMMM yyyy").withZone(ZoneId.systemDefault());

    // Modern gradient colors - Teal to Purple
    private static final Color PRIMARY_DARK = new Color(13, 71, 161);      // Deep Blue
    private static final Color PRIMARY_LIGHT = new Color(33, 150, 243);    // Light Blue
    private static final Color ACCENT_COLOR = new Color(0, 188, 212);      // Cyan
    private static final Color SUCCESS_GREEN = new Color(76, 175, 80);     // Green
    private static final Color BACKGROUND_LIGHT = new Color(250, 250, 250); // Off-white
    private static final Color TEXT_DARK = new Color(33, 33, 33);          // Almost black
    private static final Color TEXT_LIGHT = new Color(117, 117, 117);      // Gray

    @Override
    public byte[] generateReceiptPdf(Receipt receipt) {
        log.info("Generating receipt PDF for receipt number: {}", receipt.getReceiptNumber());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Modern header with gradient feel
            addModernHeader(document, receipt);
            document.add(new Paragraph("\n"));

            // Transaction details card
            addTransactionCard(document, receipt);
            document.add(new Paragraph("\n"));

            // Flight information card
            addFlightCard(document, receipt);
            document.add(new Paragraph("\n"));

            // Price breakdown card
            addPriceCard(document, receipt);
            document.add(new Paragraph("\n"));

            // Success badge
            addSuccessBadge(document);
            document.add(new Paragraph("\n"));

            // Modern footer
            addModernFooter(document);

            document.close();
            log.info("Receipt PDF generated successfully for: {}", receipt.getReceiptNumber());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating receipt PDF: {}", receipt.getReceiptNumber(), e);
            throw new RuntimeException("Failed to generate receipt PDF", e);
        }
    }

    /**
     * Modern header with logo and receipt info
     */
    private void addModernHeader(Document document, Receipt receipt) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1.5f, 1f});

        // Left side - Company branding
        PdfPCell brandCell = new PdfPCell();
        brandCell.setBorder(Rectangle.NO_BORDER);
        brandCell.setPaddingBottom(15);

        Font logoFont = new Font(Font.HELVETICA, 28, Font.BOLD, PRIMARY_DARK);
        Font taglineFont = new Font(Font.HELVETICA, 11, Font.ITALIC, ACCENT_COLOR);
        Font contactFont = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_LIGHT);

        Paragraph logo = new Paragraph("✈ AeroStar", logoFont);
        Paragraph tagline = new Paragraph("Your Journey, Our Passion", taglineFont);
        Paragraph email = new Paragraph("support@aerostar.uz", contactFont);
        Paragraph phone = new Paragraph("☎ +998 99 749 4262", contactFont);

        brandCell.addElement(logo);
        brandCell.addElement(tagline);
        brandCell.addElement(new Paragraph(" "));
        brandCell.addElement(email);
        brandCell.addElement(phone);
        headerTable.addCell(brandCell);

        // Right side - Receipt info
        PdfPCell receiptInfoCell = new PdfPCell();
        receiptInfoCell.setBorder(Rectangle.NO_BORDER);
        receiptInfoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        receiptInfoCell.setPaddingBottom(15);

        Font receiptTitleFont = new Font(Font.HELVETICA, 22, Font.BOLD, PRIMARY_LIGHT);
        Font receiptNumberFont = new Font(Font.HELVETICA, 11, Font.BOLD, TEXT_DARK);
        Font dateFont = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_LIGHT);

        Paragraph receiptTitle = new Paragraph("RECEIPT", receiptTitleFont);
        receiptTitle.setAlignment(Element.ALIGN_RIGHT);

        Paragraph receiptNum = new Paragraph("№ " + receipt.getReceiptNumber(), receiptNumberFont);
        receiptNum.setAlignment(Element.ALIGN_RIGHT);

        Paragraph date = new Paragraph(DATE_FORMAT.format(receipt.getCreatedAt()), dateFont);
        date.setAlignment(Element.ALIGN_RIGHT);

        receiptInfoCell.addElement(receiptTitle);
        receiptInfoCell.addElement(new Paragraph(" "));
        receiptInfoCell.addElement(receiptNum);
        receiptInfoCell.addElement(date);
        headerTable.addCell(receiptInfoCell);

        document.add(headerTable);

        // Decorative line
        addDecorativeLine(document, PRIMARY_LIGHT);
    }

    /**
     * Transaction details in a card style
     */
    private void addTransactionCard(Document document, Receipt receipt) throws DocumentException {
        Font cardTitleFont = new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_DARK);
        Font labelFont = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_LIGHT);
        Font valueFont = new Font(Font.HELVETICA, 10, Font.BOLD, TEXT_DARK);

        // Card title
        Paragraph title = new Paragraph("💳 Payment Details", cardTitleFont);
        title.setSpacingBefore(10);
        document.add(title);

        // Card container
        PdfPTable card = new PdfPTable(2);
        card.setWidthPercentage(100);
        card.setSpacingBefore(10);
        card.setWidths(new float[]{1f, 1.5f});

        PdfPCell containerCell = new PdfPCell();
        containerCell.setColspan(2);
        containerCell.setBackgroundColor(BACKGROUND_LIGHT);
        containerCell.setBorderColor(new Color(230, 230, 230));
        containerCell.setBorderWidth(1);
        containerCell.setPadding(15);

        // Inner table for details
        PdfPTable detailsTable = new PdfPTable(2);
        detailsTable.setWidthPercentage(100);
        detailsTable.setWidths(new float[]{1f, 1.5f});

        addCardRow(detailsTable, "Transaction ID", receipt.getTransactionId(), labelFont, valueFont);
        addCardRow(detailsTable, "Booking Reference", receipt.getBookingReference(), labelFont, valueFont);
        addCardRow(detailsTable, "Payment Method", formatPaymentMethod(receipt.getPaymentMethod()), labelFont, valueFont);

        if (receipt.getCardLastFour() != null && !receipt.getCardLastFour().equals("****")) {
            addCardRow(detailsTable, "Card", "•••• •••• •••• " + receipt.getCardLastFour(), labelFont, valueFont);
        }

        addCardRow(detailsTable, "Processed At", DATE_TIME_FORMAT.format(receipt.getPaymentDate()), labelFont, valueFont);
        addCardRow(detailsTable, "Passenger", receipt.getPassengerName(), labelFont, valueFont);

        containerCell.addElement(detailsTable);
        card.addCell(containerCell);
        document.add(card);
    }

    /**
     * Flight information card
     */
    private void addFlightCard(Document document, Receipt receipt) throws DocumentException {
        Font cardTitleFont = new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_DARK);
        Font flightFont = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_DARK);

        Paragraph title = new Paragraph("✈ Flight Information", cardTitleFont);
        title.setSpacingBefore(10);
        document.add(title);

        PdfPTable card = new PdfPTable(1);
        card.setWidthPercentage(100);
        card.setSpacingBefore(10);

        PdfPCell containerCell = new PdfPCell();
        containerCell.setBackgroundColor(BACKGROUND_LIGHT);
        containerCell.setBorderColor(new Color(230, 230, 230));
        containerCell.setBorderWidth(1);
        containerCell.setPadding(15);

        String[] flightLines = receipt.getFlightDetails().split("\n");
        for (String line : flightLines) {
            Paragraph p = new Paragraph(line, flightFont);
            p.setSpacingAfter(3);
            containerCell.addElement(p);
        }

        card.addCell(containerCell);
        document.add(card);
    }

    /**
     * Price breakdown card with modern styling
     */
    private void addPriceCard(Document document, Receipt receipt) throws DocumentException {
        Font cardTitleFont = new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_DARK);
        Font labelFont = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_DARK);
        Font valueFont = new Font(Font.HELVETICA, 10, Font.BOLD, TEXT_DARK);
        Font totalLabelFont = new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_DARK);
        Font totalValueFont = new Font(Font.HELVETICA, 18, Font.BOLD, PRIMARY_LIGHT);

        Paragraph title = new Paragraph("💰 Payment Summary", cardTitleFont);
        title.setSpacingBefore(10);
        document.add(title);

        PdfPTable card = new PdfPTable(1);
        card.setWidthPercentage(100);
        card.setSpacingBefore(10);

        PdfPCell containerCell = new PdfPCell();
        containerCell.setBackgroundColor(BACKGROUND_LIGHT);
        containerCell.setBorderColor(new Color(230, 230, 230));
        containerCell.setBorderWidth(1);
        containerCell.setPadding(20);

        // Inner price table
        PdfPTable priceTable = new PdfPTable(2);
        priceTable.setWidthPercentage(100);
        priceTable.setWidths(new float[]{2f, 1f});

        // ✅ Calculate with 1% tax instead of 5%
        BigDecimal baseFare = receipt.getAmount();
        BigDecimal tax = baseFare.multiply(BigDecimal.valueOf(0.01)); // 1% tax
        BigDecimal total = baseFare.add(tax);

        addPriceRow(priceTable, "Base Fare", formatCurrency(baseFare, receipt.getCurrency()), labelFont, valueFont);
        addPriceRow(priceTable, "Tax (1%)", formatCurrency(tax, receipt.getCurrency()), labelFont, valueFont);

        // Space before total
        PdfPCell spacer1 = new PdfPCell();
        spacer1.setBorder(Rectangle.NO_BORDER);
        spacer1.setFixedHeight(10);
        PdfPCell spacer2 = new PdfPCell();
        spacer2.setBorder(Rectangle.NO_BORDER);
        spacer2.setFixedHeight(10);
        priceTable.addCell(spacer1);
        priceTable.addCell(spacer2);

        // Total with colored background
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL PAID", totalLabelFont));
        totalLabelCell.setBorder(Rectangle.NO_BORDER);
        totalLabelCell.setBackgroundColor(new Color(240, 248, 255));
        totalLabelCell.setPadding(10);
        priceTable.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(new Phrase(formatCurrency(total, receipt.getCurrency()), totalValueFont));
        totalValueCell.setBorder(Rectangle.NO_BORDER);
        totalValueCell.setBackgroundColor(new Color(240, 248, 255));
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValueCell.setPadding(10);
        priceTable.addCell(totalValueCell);

        containerCell.addElement(priceTable);
        card.addCell(containerCell);
        document.add(card);
    }

    /**
     * Success badge
     */
    private void addSuccessBadge(Document document) throws DocumentException {
        PdfPTable badgeTable = new PdfPTable(1);
        badgeTable.setWidthPercentage(100);
        badgeTable.setSpacingBefore(15);

        PdfPCell badgeCell = new PdfPCell();
        badgeCell.setBackgroundColor(new Color(232, 245, 233));
        badgeCell.setBorderColor(SUCCESS_GREEN);
        badgeCell.setBorderWidth(2);
        badgeCell.setPadding(15);
        badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Font successFont = new Font(Font.HELVETICA, 16, Font.BOLD, SUCCESS_GREEN);
        Paragraph successText = new Paragraph("✓ PAYMENT CONFIRMED", successFont);
        successText.setAlignment(Element.ALIGN_CENTER);

        Font infoFont = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_LIGHT);
        Paragraph info = new Paragraph("Your booking has been confirmed and tickets have been issued", infoFont);
        info.setAlignment(Element.ALIGN_CENTER);

        badgeCell.addElement(successText);
        badgeCell.addElement(info);

        badgeTable.addCell(badgeCell);
        document.add(badgeTable);
    }

    /**
     * Modern footer
     */
    private void addModernFooter(Document document) throws DocumentException {
        Font footerTitleFont = new Font(Font.HELVETICA, 9, Font.BOLD, TEXT_DARK);
        Font footerFont = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_LIGHT);
        Font thankYouFont = new Font(Font.HELVETICA, 11, Font.ITALIC, PRIMARY_LIGHT);

        // Separator
        addDecorativeLine(document, new Color(230, 230, 230));

        Paragraph terms = new Paragraph("Important Information", footerTitleFont);
        terms.setSpacingBefore(15);
        terms.setSpacingAfter(5);
        document.add(terms);

        Paragraph termsText = new Paragraph(
                "• Keep this receipt for your records\n" +
                        "• For inquiries: support@aerostar.uz or +998 99 749 4262\n" +
                        "• Refunds are subject to fare rules and may take 5-10 business days\n" +
                        "• This is a computer-generated receipt and does not require a signature",
                footerFont
        );
        document.add(termsText);

        Paragraph thankYou = new Paragraph(
                "\nThank you for flying with AeroStar Airlines! ✈",
                thankYouFont
        );
        thankYou.setAlignment(Element.ALIGN_CENTER);
        thankYou.setSpacingBefore(20);
        document.add(thankYou);
    }

    /**
     * Helper: Add card row with label and value
     */
    private void addCardRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(8);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(8);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    /**
     * Helper: Add price row
     */
    public void addPriceRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(6);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPaddingBottom(6);
        table.addCell(valueCell);
    }

    /**
     * Helper: Add decorative line
     */
    private void addDecorativeLine(Document document, Color color) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        line.setSpacingBefore(10);
        line.setSpacingAfter(10);

        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.BOTTOM);
        lineCell.setBorderColor(color);
        lineCell.setBorderWidth(2);
        lineCell.setFixedHeight(1);
        line.addCell(lineCell);

        document.add(line);
    }

    /**
     * Format payment method
     */
    public String formatPaymentMethod(String method) {
        if (method == null) return "N/A";
        return switch (method) {
            case "CREDIT_CARD" -> "Credit Card";
            case "DEBIT_CARD" -> "Debit Card";
            case "PAYPAL" -> "PayPal";
            case "BANK_TRANSFER" -> "Bank Transfer";
            default -> method;
        };
    }

    /**
     * Format currency
     */
    public String formatCurrency(BigDecimal amount, String currency) {
        return String.format("%s %.2f", currency, amount);
    }
}
