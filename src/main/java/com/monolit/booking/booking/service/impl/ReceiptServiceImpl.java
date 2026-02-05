package com.monolit.booking.booking.service.impl;

import com.monolit.booking.booking.dto.response.ReceiptResponse;
import com.monolit.booking.booking.entity.*;
import com.monolit.booking.booking.exception.BookingNotFoundException;
import com.monolit.booking.booking.exception.PaymentNotFoundException;
import com.monolit.booking.booking.repo.BookingRepository;
import com.monolit.booking.booking.repo.FlightRepository;
import com.monolit.booking.booking.repo.ReceiptRepository;
import com.monolit.booking.booking.service.interfaces.ReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final ReceiptPdfService receiptPdfService;

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.12"); // 12% tax
    private static final BigDecimal SERVICE_FEE_RATE = new BigDecimal("0.03"); // 3% service fee

    @Override
    @Transactional
    public ReceiptResponse createReceipt(Payment payment, Booking booking) {
        log.info("Creating receipt for payment: {}", payment.getTransactionId());

        // Calculate amounts
        BigDecimal baseAmount = payment.getAmount();
        BigDecimal taxAmount = baseAmount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal serviceFee = baseAmount.multiply(SERVICE_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = baseAmount.add(taxAmount).add(serviceFee);

        // Build passenger names
        String passengerNames = buildPassengerNames(booking);

        // Build flight details
        String flightDetails = buildFlightDetails(booking);

        Receipt receipt = Receipt.builder()
                .receiptNumber(generateReceiptNumber())
                .paymentId(payment.getId())
                .transactionId(payment.getTransactionId())
                .bookingReference(booking.getBookingReference())
                .userId(booking.getUserId())
                .amount(baseAmount)
                .taxAmount(taxAmount)
                .serviceFee(serviceFee)
                .totalAmount(totalAmount)
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod().name())
                .cardLastFour(payment.getCardLastFour())
                .passengerName(passengerNames)
                .flightDetails(flightDetails)
                .paymentDate(payment.getProcessedAt())
                .build();

        receipt = receiptRepository.save(receipt);
        log.info("Receipt created with number: {}", receipt.getReceiptNumber());

        return mapToResponse(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptByBookingReference(String bookingReference, Long userId) {
        log.info("Getting receipt for booking: {}", bookingReference);

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new BookingNotFoundException(bookingReference, true));

        if (!booking.getUserId().equals(userId)) {
            throw new BookingNotFoundException("Booking not found or access denied");
        }

        Receipt receipt = receiptRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new PaymentNotFoundException("Receipt not found for booking: " + bookingReference));

        return mapToResponse(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptByTransactionId(String transactionId) {
        log.info("Getting receipt for transaction: {}", transactionId);

        Receipt receipt = receiptRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new PaymentNotFoundException("Receipt not found for transaction: " + transactionId));

        return mapToResponse(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateReceiptPdf(String bookingReference, Long userId) {
        log.info("Generating receipt PDF for booking: {}", bookingReference);

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new BookingNotFoundException(bookingReference, true));

        if (!booking.getUserId().equals(userId)) {
            throw new BookingNotFoundException("Booking not found or access denied");
        }

        Receipt receipt = receiptRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new PaymentNotFoundException("Receipt not found for booking: " + bookingReference));

        return receiptPdfService.generateReceiptPdf(receipt);
    }

    private String generateReceiptNumber() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return "RCP-" + uuid;
    }

    private String buildPassengerNames(Booking booking) {
        if (booking.getPassengers() == null || booking.getPassengers().isEmpty()) {
            return "N/A";
        }

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Passenger passenger : booking.getPassengers()) {
            if (count > 0) sb.append(", ");
            sb.append(passenger.getFirstName()).append(" ").append(passenger.getLastName());
            count++;
        }
        return sb.toString();
    }

    private String buildFlightDetails(Booking booking) {
        if (booking.getBookingFlights() == null || booking.getBookingFlights().isEmpty()) {
            return "N/A";
        }

        StringBuilder sb = new StringBuilder();
        for (BookingFlight bookingFlight : booking.getBookingFlights()) {
            Flight flight = flightRepository.findById(bookingFlight.getFlightId()).orElse(null);
            if (flight != null) {
                sb.append("Flight: ").append(flight.getFlightNumber());
                sb.append(" | ").append(flight.getAirline().getName());
                sb.append("\n");
                sb.append("Route: ").append(flight.getDepartureAirport().getCity());
                sb.append(" (").append(flight.getDepartureAirport().getIataCode()).append(")");
                sb.append(" → ").append(flight.getArrivalAirport().getCity());
                sb.append(" (").append(flight.getArrivalAirport().getIataCode()).append(")");
                sb.append("\n");
                sb.append("Departure: ").append(flight.getDepartureTime().format(DATE_TIME_FORMAT));
                sb.append("\n");
                sb.append("Class: ").append(bookingFlight.getSeatClass().name());
                sb.append(" | Passengers: ").append(bookingFlight.getPassengerCount());
                sb.append("\n");
            }
        }
        return sb.toString().trim();
    }

    private ReceiptResponse mapToResponse(Receipt receipt) {
        return ReceiptResponse.builder()
                .id(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .transactionId(receipt.getTransactionId())
                .bookingReference(receipt.getBookingReference())
                .amount(receipt.getAmount())
                .taxAmount(receipt.getTaxAmount())
                .serviceFee(receipt.getServiceFee())
                .totalAmount(receipt.getTotalAmount())
                .currency(receipt.getCurrency())
                .paymentMethod(receipt.getPaymentMethod())
                .cardLastFour(receipt.getCardLastFour())
                .passengerName(receipt.getPassengerName())
                .flightDetails(receipt.getFlightDetails())
                .createdAt(receipt.getCreatedAt())
                .paymentDate(receipt.getPaymentDate())
                .build();
    }
}
