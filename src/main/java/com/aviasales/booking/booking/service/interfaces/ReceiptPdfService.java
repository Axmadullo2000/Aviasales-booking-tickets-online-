package com.aviasales.booking.booking.service.interfaces;

import com.aviasales.booking.booking.entity.Receipt;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.math.BigDecimal;


public interface ReceiptPdfService {
    byte[] generateReceiptPdf(Receipt receipt);

    void addPriceRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont);

    String formatPaymentMethod(String method);

    String formatCurrency(BigDecimal amount, String currency);

}
