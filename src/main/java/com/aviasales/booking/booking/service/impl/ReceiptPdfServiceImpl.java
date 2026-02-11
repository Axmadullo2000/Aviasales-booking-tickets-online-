package com.aviasales.booking.booking.service.impl;

import com.aviasales.booking.booking.entity.Receipt;
import com.aviasales.booking.booking.service.interfaces.ReceiptPdfService;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


@Slf4j
@Service
public class ReceiptPdfServiceImpl implements ReceiptPdfService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter
            .ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("dd MMMM yyyy").withZone(ZoneId.systemDefault());

    // Uzbekistan Airways brand colors
    private static final Color UA_DARK_BLUE  = new Color(0,   48,  135); // #003087
    private static final Color UA_MID_BLUE   = new Color(0,   87,  168); // #0057A8
    private static final Color UA_GOLD       = new Color(200, 168,  75); // #C8A84B
    private static final Color UA_LIGHT_GOLD = new Color(232, 208, 139); // #E8D08B
    private static final Color UA_BG         = new Color(245, 247, 250); // #F5F7FA
    private static final Color UA_LINE       = new Color(224, 229, 238); // #E0E5EE
    private static final Color UA_TEXT_DARK  = new Color( 44,  44,  44);
    private static final Color UA_TEXT_GRAY  = new Color(139, 139, 139);
    private static final Color UA_BLUE_TINT  = new Color(238, 242, 248);

    private static final BigDecimal TAX_RATE     = new BigDecimal("0.005"); // 0.5%
    private static final BigDecimal SERVICE_RATE = new BigDecimal("0.005"); // 0.5%

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public byte[] generateReceiptPdf(Receipt receipt) {
        log.info("Generating UA-style receipt PDF for: {}", receipt.getReceiptNumber());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // ── PAGE 1 ──────────────────────────────────────────────────────
            addHeader(document, writer, receipt);
            addSpacer(document, 6);
            addBookingInfoCard(document, receipt);
            addSpacer(document, 6);
            addFlightCard(document, receipt);
            addSpacer(document, 6);
            addPassengerCard(document, receipt);
            addSpacer(document, 6);
            addPaymentSummaryCard(document, receipt);
            addFooter(document);

            // ── PAGE 2 ──────────────────────────────────────────────────────
            document.newPage();
            addPage2Header(document, writer, receipt);
            addSpacer(document, 6);
            addFareBreakdownCard(document, receipt);
            addSpacer(document, 6);
            addConditionsCard(document);
            addSpacer(document, 6);
            addContactCard(document);
            addFooter(document);

            document.close();
            log.info("Receipt PDF generated successfully: {}", receipt.getReceiptNumber());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating receipt PDF for: {}", receipt.getReceiptNumber(), e);
            throw new RuntimeException("Failed to generate receipt PDF", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAGE 1 SECTIONS
    // ─────────────────────────────────────────────────────────────────────────

    /** Dark-blue header banner */
    private void addHeader(Document doc, PdfWriter writer, Receipt receipt) throws DocumentException {
        PdfContentByte cb = writer.getDirectContent();

        // Dark blue background
        cb.setColorFill(UA_DARK_BLUE);
        cb.rectangle(36, PageSize.A4.getHeight() - 36 - 80, PageSize.A4.getWidth() - 72, 80);
        cb.fill();

        // Gold accent strip below header
        cb.setColorFill(UA_GOLD);
        cb.rectangle(36, PageSize.A4.getHeight() - 36 - 83, PageSize.A4.getWidth() - 72, 3);
        cb.fill();

        // Airline name
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1.8f, 1f});

        PdfPCell left = noBorder();
        left.setBackgroundColor(UA_DARK_BLUE);
        left.setPadding(14);

        Paragraph airlineName = new Paragraph("UZBEKISTAN AIRWAYS",
                font(Font.HELVETICA, 20, Font.BOLD, Color.WHITE));
        Paragraph tagline = new Paragraph("O'zbekiston havo yo'llari",
                font(Font.HELVETICA, 9, Font.ITALIC, UA_LIGHT_GOLD));

        Paragraph refLabel = new Paragraph("Receipt No.",
                font(Font.HELVETICA, 8, Font.NORMAL, new Color(184, 200, 232)));
        Paragraph refValue = new Paragraph(safe(receipt.getReceiptNumber()),
                font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE));

        left.addElement(airlineName);
        left.addElement(tagline);
        left.addElement(new Paragraph(" "));
        left.addElement(refLabel);
        left.addElement(refValue);
        header.addCell(left);

        PdfPCell right = noBorder();
        right.setBackgroundColor(UA_DARK_BLUE);
        right.setPadding(14);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);

        // Gold RECEIPT badge (simulated with colored cell)
        Paragraph receiptBadge = new Paragraph("RECEIPT",
                font(Font.HELVETICA, 18, Font.BOLD, UA_GOLD));
        receiptBadge.setAlignment(Element.ALIGN_RIGHT);

        Paragraph dateLabel = new Paragraph("Issued",
                font(Font.HELVETICA, 8, Font.NORMAL, new Color(184, 200, 232)));
        dateLabel.setAlignment(Element.ALIGN_RIGHT);

        String issuedDate = receipt.getCreatedAt() != null
                ? DATE_FORMAT.format(receipt.getCreatedAt()) : "—";
        Paragraph dateValue = new Paragraph(issuedDate,
                font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE));
        dateValue.setAlignment(Element.ALIGN_RIGHT);

        right.addElement(receiptBadge);
        right.addElement(new Paragraph(" "));
        right.addElement(dateLabel);
        right.addElement(dateValue);
        header.addCell(right);

        doc.add(header);
        addSpacer(doc, 4); // space consumed by gold strip drawn via cb
    }

    /** Booking reference, transaction, contact */
    private void addBookingInfoCard(Document doc, Receipt receipt) throws DocumentException {
        addSectionTitle(doc, "BOOKING INFORMATION");

        PdfPTable card = new PdfPTable(2);
        card.setWidthPercentage(100);
        card.setWidths(new float[]{1f, 1f});
        card.setSpacingBefore(4);

        String[][] left = {
                {"Booking Reference", safe(receipt.getBookingReference())},
                {"Payment Method",    formatPaymentMethod(receipt.getPaymentMethod())},
                {"Card",              receipt.getCardLastFour() != null
                        ? "\u2022\u2022\u2022\u2022 " + receipt.getCardLastFour() : "N/A"},
        };
        String[][] right = {
                {"Transaction ID",    safe(receipt.getTransactionId())},
                {"Payment Date",      receipt.getPaymentDate() != null
                        ? DATE_TIME_FORMAT.format(receipt.getPaymentDate()) : "—"},
                {"Status",            "CONFIRMED"},
        };

        // All rows in one background cell
        PdfPCell bg = new PdfPCell();
        bg.setColspan(2);
        bg.setBackgroundColor(UA_BG);
        bg.setBorderColor(UA_LINE);
        bg.setBorderWidth(0.5f);
        bg.setPadding(14);

        PdfPTable inner = new PdfPTable(4);
        inner.setWidthPercentage(100);
        inner.setWidths(new float[]{1f, 1.4f, 1f, 1.4f});

        for (int i = 0; i < left.length; i++) {
            innerLabelCell(inner, left[i][0]);
            innerValueCell(inner, left[i][1], i == 2 && "CONFIRMED".equals(left[i][1]));
            innerLabelCell(inner, right[i][0]);
            innerValueCell(inner, right[i][1], "CONFIRMED".equals(right[i][1]));
        }

        bg.addElement(inner);
        card.addCell(bg);
        doc.add(card);
    }

    /** Flight details parsed from flightDetails string */
    private void addFlightCard(Document doc, Receipt receipt) throws DocumentException {
        addSectionTitle(doc, "FLIGHT DETAILS");

        PdfPTable card = new PdfPTable(1);
        card.setWidthPercentage(100);
        card.setSpacingBefore(4);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(UA_BG);
        cell.setBorderColor(UA_LINE);
        cell.setBorderWidth(0.5f);
        cell.setPadding(14);

        if (receipt.getFlightDetails() != null && !receipt.getFlightDetails().isBlank()) {
            String[] lines = receipt.getFlightDetails().split("\n");
            for (String line : lines) {
                if (line.isBlank()) continue;
                Font f = line.startsWith("Flight:") || line.startsWith("Route:")
                        ? font(Font.HELVETICA, 10, Font.BOLD, UA_DARK_BLUE)
                        : font(Font.HELVETICA, 9, Font.NORMAL, UA_TEXT_DARK);
                Paragraph p = new Paragraph(line.trim(), f);
                p.setSpacingAfter(3);
                cell.addElement(p);
            }
        } else {
            cell.addElement(new Paragraph("No flight information available.",
                    font(Font.HELVETICA, 9, Font.ITALIC, UA_TEXT_GRAY)));
        }

        card.addCell(cell);
        doc.add(card);
    }

    /** Passenger name */
    private void addPassengerCard(Document doc, Receipt receipt) throws DocumentException {
        addSectionTitle(doc, "PASSENGER DETAILS");

        PdfPTable card = new PdfPTable(2);
        card.setWidthPercentage(100);
        card.setWidths(new float[]{1f, 1f});
        card.setSpacingBefore(4);

        PdfPCell bg = new PdfPCell();
        bg.setColspan(2);
        bg.setBackgroundColor(UA_BG);
        bg.setBorderColor(UA_LINE);
        bg.setBorderWidth(0.5f);
        bg.setPadding(14);

        PdfPTable inner = new PdfPTable(2);
        inner.setWidthPercentage(100);
        inner.setWidths(new float[]{1f, 2f});

        innerLabelCell(inner, "Full Name");
        innerValueCell(inner, safe(receipt.getPassengerName()), false);

        bg.addElement(inner);
        card.addCell(bg);
        doc.add(card);
    }

    /** Payment summary with 0.5% tax + 0.5% service fee */
    private void addPaymentSummaryCard(Document doc, Receipt receipt) throws DocumentException {
        addSectionTitle(doc, "PAYMENT SUMMARY");

        BigDecimal base       = receipt.getAmount();
        BigDecimal tax        = base.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal serviceFee = base.multiply(SERVICE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total      = base.add(tax).add(serviceFee);
        String cur            = safe(receipt.getCurrency());

        PdfPTable card = new PdfPTable(1);
        card.setWidthPercentage(100);
        card.setSpacingBefore(4);

        PdfPCell bg = new PdfPCell();
        bg.setBackgroundColor(UA_BG);
        bg.setBorderColor(UA_LINE);
        bg.setBorderWidth(0.5f);
        bg.setPadding(16);

        PdfPTable rows = new PdfPTable(2);
        rows.setWidthPercentage(100);
        rows.setWidths(new float[]{2f, 1f});

        addPriceRow(rows, "Base Fare",          formatCurrency(base,       cur),
                font(Font.HELVETICA, 10, Font.NORMAL, UA_TEXT_DARK),
                font(Font.HELVETICA, 10, Font.NORMAL, UA_TEXT_DARK));
        addPriceRow(rows, "Service Fee (0.5%)",  formatCurrency(serviceFee, cur),
                font(Font.HELVETICA, 10, Font.NORMAL, UA_TEXT_DARK),
                font(Font.HELVETICA, 10, Font.NORMAL, UA_TEXT_DARK));

        // Gold divider row
        PdfPCell div1 = new PdfPCell(); div1.setColspan(2);
        div1.setBorderWidthTop(0); div1.setBorderWidthBottom(1.2f);
        div1.setBorderColor(UA_GOLD); div1.setFixedHeight(8);
        div1.setBorderWidthLeft(0); div1.setBorderWidthRight(0);
        rows.addCell(div1);

        // Total row
        PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL AMOUNT",
                font(Font.HELVETICA, 13, Font.BOLD, UA_DARK_BLUE)));
        totalLabel.setBorder(Rectangle.NO_BORDER);
        totalLabel.setBackgroundColor(UA_BLUE_TINT);
        totalLabel.setPadding(10);
        rows.addCell(totalLabel);

        PdfPCell totalValue = new PdfPCell(new Phrase(formatCurrency(total, cur),
                font(Font.HELVETICA, 14, Font.BOLD, UA_GOLD)));
        totalValue.setBorder(Rectangle.NO_BORDER);
        totalValue.setBackgroundColor(UA_BLUE_TINT);
        totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValue.setPadding(10);
        rows.addCell(totalValue);

        bg.addElement(rows);

        Paragraph note = new Paragraph("Includes all applicable taxes and fees  \u2022  Currency: " + cur,
                font(Font.HELVETICA, 8, Font.ITALIC, UA_TEXT_GRAY));
        note.setSpacingBefore(8);
        bg.addElement(note);

        card.addCell(bg);
        doc.add(card);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAGE 2 SECTIONS
    // ─────────────────────────────────────────────────────────────────────────

    private void addPage2Header(Document doc, PdfWriter writer, Receipt receipt) throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{2f, 1f});

        PdfPCell left = noBorder();
        left.setBackgroundColor(UA_DARK_BLUE);
        left.setPadding(12);
        Paragraph title = new Paragraph("UZBEKISTAN AIRWAYS",
                font(Font.HELVETICA, 16, Font.BOLD, Color.WHITE));
        Paragraph sub = new Paragraph("Payment Receipt \u2014 Page 2 of 2",
                font(Font.HELVETICA, 9, Font.ITALIC, UA_LIGHT_GOLD));
        left.addElement(title);
        left.addElement(sub);
        header.addCell(left);

        PdfPCell right = noBoard_Right();
        right.setBackgroundColor(UA_DARK_BLUE);
        right.setPadding(12);
        Paragraph ref = new Paragraph(safe(receipt.getReceiptNumber()),
                font(Font.HELVETICA, 10, Font.BOLD, UA_GOLD));
        ref.setAlignment(Element.ALIGN_RIGHT);
        Paragraph bk = new Paragraph(safe(receipt.getBookingReference()),
                font(Font.HELVETICA, 8, Font.NORMAL, new Color(184, 200, 232)));
        bk.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(ref);
        right.addElement(bk);
        header.addCell(right);

        // Gold strip
        PdfPTable strip = new PdfPTable(1);
        strip.setWidthPercentage(100);
        PdfPCell stripCell = new PdfPCell();
        stripCell.setFixedHeight(3);
        stripCell.setBackgroundColor(UA_GOLD);
        stripCell.setBorder(Rectangle.NO_BORDER);
        strip.addCell(stripCell);

        doc.add(header);
        doc.add(strip);
    }

    private void addFareBreakdownCard(Document doc, Receipt receipt) throws DocumentException {
        addSectionTitle(doc, "DETAILED FARE BREAKDOWN");

        BigDecimal base       = receipt.getAmount();
        BigDecimal tax        = base.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal serviceFee = base.multiply(SERVICE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total      = base.add(tax).add(serviceFee);
        String cur            = safe(receipt.getCurrency());

        PdfPTable card = new PdfPTable(1);
        card.setWidthPercentage(100);
        card.setSpacingBefore(4);

        PdfPCell bg = new PdfPCell();
        bg.setBackgroundColor(UA_BG);
        bg.setBorderColor(UA_LINE);
        bg.setBorderWidth(0.5f);
        bg.setPadding(14);

        // Column headers
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 2.5f, 1.2f});

        for (String h : new String[]{"ITEM", "DESCRIPTION", "AMOUNT"}) {
            PdfPCell hc = new PdfPCell(new Phrase(h,
                    font(Font.HELVETICA, 8, Font.BOLD, UA_DARK_BLUE)));
            hc.setBackgroundColor(UA_BLUE_TINT);
            hc.setBorderColor(UA_LINE);
            hc.setBorderWidth(0.5f);
            hc.setPadding(7);
            if ("AMOUNT".equals(h)) hc.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(hc);
        }

        Object[][] items = {
                {"Base Ticket Price",   "Fare class included in booking",    formatCurrency(base, cur)},
                {"Airport Tax (0.5%)",  "Government-mandated airport levy",  formatCurrency(tax, cur)},
                {"Fuel Surcharge",      "Included in base fare",             "Included"},
                {"Baggage Allowance",   "1x23kg checked + 1x10kg cabin",      "Included"},
        };

        for (Object[] row : items) {
            tableCell(table, (String) row[0], Font.BOLD,   UA_TEXT_DARK, false);
            tableCell(table, (String) row[1], Font.NORMAL, UA_TEXT_GRAY,  false);
            tableCellRight(table, (String) row[2], Font.NORMAL, UA_TEXT_DARK, false);
        }

        // Total row
        PdfPCell tc1 = new PdfPCell(new Phrase("TOTAL CHARGED",
                font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
        tc1.setColspan(2);
        tc1.setBackgroundColor(UA_DARK_BLUE);
        tc1.setBorder(Rectangle.NO_BORDER);
        tc1.setPadding(9);
        table.addCell(tc1);

        PdfPCell tc2 = new PdfPCell(new Phrase(formatCurrency(total, cur),
                font(Font.HELVETICA, 10, Font.BOLD, UA_GOLD)));
        tc2.setBackgroundColor(UA_DARK_BLUE);
        tc2.setBorder(Rectangle.NO_BORDER);
        tc2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tc2.setPadding(9);
        table.addCell(tc2);

        bg.addElement(table);
        card.addCell(bg);
        doc.add(card);
    }

    private void addConditionsCard(Document doc) throws DocumentException {
        addSectionTitle(doc, "CONDITIONS & POLICIES");

        PdfPTable card = new PdfPTable(1);
        card.setWidthPercentage(100);
        card.setSpacingBefore(4);

        PdfPCell bg = new PdfPCell();
        bg.setBackgroundColor(UA_BG);
        bg.setBorderColor(UA_LINE);
        bg.setBorderWidth(0.5f);
        bg.setPadding(14);

        String[] conditions = {
                "Refund Policy: Tickets are refundable subject to applicable cancellation fees and fare rules.",
                "Rebooking: Date changes are permitted for a fee per segment, subject to availability.",
                "Baggage: 1 checked bag (23 kg) and 1 cabin bag (10 kg) included in the fare.",
                "Check-in: Online check-in opens 24 hours before scheduled departure.",
                "Documentation: Passengers must present valid passport or national ID at check-in.",
                "Liability: Governed by the Warsaw / Montreal Convention on international carriage.",
        };

        for (String line : conditions) {
            PdfPTable row = new PdfPTable(2);
            row.setWidthPercentage(100);
            row.setWidths(new float[]{0.05f, 1f});

            PdfPCell bullet = new PdfPCell(new Phrase("\u2022",
                    font(Font.HELVETICA, 10, Font.BOLD, UA_GOLD)));
            bullet.setBorder(Rectangle.NO_BORDER);
            bullet.setPaddingBottom(5);
            row.addCell(bullet);

            PdfPCell text = new PdfPCell(new Phrase(line,
                    font(Font.HELVETICA, 8.5f, Font.NORMAL, UA_TEXT_DARK)));
            text.setBorder(Rectangle.NO_BORDER);
            text.setPaddingBottom(5);
            row.addCell(text);

            bg.addElement(row);
        }

        card.addCell(bg);
        doc.add(card);
    }

    private void addContactCard(Document doc) throws DocumentException {
        addSectionTitle(doc, "CUSTOMER SUPPORT");

        PdfPTable card = new PdfPTable(4);
        card.setWidthPercentage(100);
        card.setWidths(new float[]{1f, 1f, 1f, 1f});
        card.setSpacingBefore(4);

        String[][] contacts = {
                {"Phone",   "+998 99 749 42 62"},
                {"Email",   "axmadullo2000@gmail.com"},
                {"Website", "https://github.com/Axmadullo2000"},
                {"Address", "4 Navro'z Yangi Darxon, Tashkent"},
        };

        for (String[] c : contacts) {
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(UA_BLUE_TINT);
            cell.setBorderColor(UA_LINE);
            cell.setBorderWidth(0.5f);
            cell.setPadding(10);

            Paragraph label = new Paragraph(c[0],
                    font(Font.HELVETICA, 7.5f, Font.NORMAL, UA_TEXT_GRAY));
            Paragraph value = new Paragraph(c[1],
                    font(Font.HELVETICA, 8.5f, Font.BOLD, UA_DARK_BLUE));
            cell.addElement(label);
            cell.addElement(value);
            card.addCell(cell);
        }

        doc.add(card);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FOOTER
    // ─────────────────────────────────────────────────────────────────────────

    private void addFooter(Document doc) throws DocumentException {
        addSpacer(doc, 10);

        PdfPTable footer = new PdfPTable(1);
        footer.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(238, 242, 248));
        cell.setBorderColor(UA_LINE);
        cell.setBorderWidth(0.5f);
        cell.setPadding(10);

        Paragraph p1 = new Paragraph(
                "\u2022 This receipt is computer-generated and serves as official proof of payment.\n" +
                        "\u2022 For inquiries: support@uzairways.com  |  +998 99 749 42 62\n" +
                        "\u2022 Refunds subject to fare rules and may take 5\u201310 business days.",
                font(Font.HELVETICA, 7.5f, Font.NORMAL, UA_TEXT_GRAY));
        cell.addElement(p1);

        Paragraph thanks = new Paragraph(
                "\u2728  Thank you for flying with Uzbekistan Airways! \u2708",
                font(Font.HELVETICA, 9, Font.ITALIC, UA_MID_BLUE));
        thanks.setAlignment(Element.ALIGN_CENTER);
        thanks.setSpacingBefore(6);
        cell.addElement(thanks);

        footer.addCell(cell);
        doc.add(footer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERFACE HELPERS (also public for interface contract)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void addPriceRow(PdfPTable table, String label, String value,
                            Font labelFont, Font valueFont) {
        PdfPCell lc = new PdfPCell(new Phrase(label, labelFont));
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setPaddingBottom(6);
        table.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vc.setPaddingBottom(6);
        table.addCell(vc);
    }

    @Override
    public String formatPaymentMethod(String method) {
        if (method == null) return "N/A";
        return switch (method) {
            case "CREDIT_CARD"   -> "Credit Card";
            case "DEBIT_CARD"    -> "Debit Card";
            case "PAYPAL"        -> "PayPal";
            case "BANK_TRANSFER" -> "Bank Transfer";
            default              -> method;
        };
    }

    @Override
    public String formatCurrency(BigDecimal amount, String currency) {
        if (amount == null) return currency + " 0.00";
        return String.format("%s %.2f", currency, amount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE UTILITIES
    // ─────────────────────────────────────────────────────────────────────────

    private void addSectionTitle(Document doc, String text) throws DocumentException {
        PdfPTable title = new PdfPTable(1);
        title.setWidthPercentage(100);
        title.setSpacingBefore(6);

        PdfPCell cell = new PdfPCell(new Phrase(text,
                font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
        cell.setBackgroundColor(UA_DARK_BLUE);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingLeft(10);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(5);
        title.addCell(cell);

        doc.add(title);
    }

    private void innerLabelCell(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text,
                font(Font.HELVETICA, 7.5f, Font.NORMAL, UA_TEXT_GRAY)));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingBottom(8);
        c.setPaddingRight(6);
        t.addCell(c);
    }

    private void innerValueCell(PdfPTable t, String text, boolean highlight) {
        Font f = highlight
                ? font(Font.HELVETICA, 9, Font.BOLD, new Color(26, 122, 63))
                : font(Font.HELVETICA, 9, Font.BOLD, UA_TEXT_DARK);
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingBottom(8);
        t.addCell(c);
    }

    private void tableCell(PdfPTable t, String text, int style, Color color, boolean right) {
        PdfPCell c = new PdfPCell(new Phrase(text, font(Font.HELVETICA, 8.5f, style, color)));
        c.setBorderColor(UA_LINE);
        c.setBorderWidth(0.3f);
        c.setPadding(6);
        if (right) c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c);
    }

    private void tableCellRight(PdfPTable t, String text, int style, Color color, boolean dummy) {
        PdfPCell c = new PdfPCell(new Phrase(text, font(Font.HELVETICA, 8.5f, style, color)));
        c.setBorderColor(UA_LINE);
        c.setBorderWidth(0.3f);
        c.setPadding(6);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c);
    }

    private Font font(int family, float size, int style, Color color) {
        return new Font(family, size, style, color);
    }

    private PdfPCell noBoard_Right() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private PdfPCell noBorder() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private void addSpacer(Document doc, float height) throws DocumentException {
        Paragraph sp = new Paragraph(" ");
        sp.setSpacingAfter(height);
        doc.add(sp);
    }

    private String safe(String value) {
        return value != null ? value : "—";
    }
}
