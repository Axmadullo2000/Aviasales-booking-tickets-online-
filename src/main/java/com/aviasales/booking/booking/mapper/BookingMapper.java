package com.aviasales.booking.booking.mapper;

import com.aviasales.booking.booking.dto.request.PassengerInfoRequest;
import com.aviasales.booking.booking.dto.response.BookingDetailResponse;
import com.aviasales.booking.booking.dto.response.BookingResponse;
import com.aviasales.booking.booking.dto.response.ContactInfoResponse;
import com.aviasales.booking.booking.dto.response.TicketResponse;
import com.aviasales.booking.booking.embedded.ContactInfo;
import com.aviasales.booking.booking.entity.Booking;
import com.aviasales.booking.booking.entity.Passenger;
import com.aviasales.booking.booking.entity.Ticket;
import com.aviasales.booking.booking.enums.BookingStatus;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface BookingMapper {

    // ═══════════════════════════════════════
    // BOOKING → BOOKING RESPONSE
    // ═══════════════════════════════════════

    @Mapping(target = "totalPassengers", expression = "java(booking.getTotalPassengers())")
    @Mapping(target = "amountDue", expression = "java(calculateAmountDue(booking))")
    @Mapping(target = "paymentInstructions", expression = "java(generatePaymentInstructions(booking))")
    BookingResponse toBookingResponse(Booking booking);

    // ═══════════════════════════════════════
    // BOOKING → BOOKING DETAIL RESPONSE
    // ═══════════════════════════════════════

    @Mapping(target = "amountDue", expression = "java(calculateAmountDue(booking))")
    @Mapping(target = "tickets", source = "tickets")
    @Mapping(target = "contactInfo", source = "contactInfo")
    BookingDetailResponse toBookingDetailResponse(Booking booking);

    // ═══════════════════════════════════════
    // CONTACT INFO
    // ═══════════════════════════════════════

    ContactInfoResponse toContactInfoResponse(ContactInfo contactInfo);

    // ═══════════════════════════════════════
    // TICKET → TICKET RESPONSE
    // ═══════════════════════════════════════

    @Mapping(target = "passengerName", expression = "java(getPassengerFullName(ticket))")
    @Mapping(target = "passengerFirstName", source = "passenger.firstName")
    @Mapping(target = "passengerLastName", source = "passenger.lastName")
    @Mapping(target = "flightNumber", source = "flight.flightNumber")
    @Mapping(target = "origin", source = "flight.origin.iataCode")
    @Mapping(target = "destination", source = "flight.destination.iataCode")
    @Mapping(target = "departureTime", source = "flight.departureTime")
    @Mapping(target = "arrivalTime", source = "flight.arrivalTime")
    TicketResponse toTicketResponse(Ticket ticket);

    List<TicketResponse> toTicketResponseList(List<Ticket> tickets);

    // ═══════════════════════════════════════
    // PASSENGER INFO REQUEST → PASSENGER
    // ═══════════════════════════════════════

    Passenger toPassenger(PassengerInfoRequest request);

    // ═══════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════

    /**
     * Получить полное имя пассажира
     */
    default String getPassengerFullName(Ticket ticket) {
        if (ticket.getPassenger() == null) {
            return null;
        }
        return ticket.getPassenger().getFirstName() + " " + ticket.getPassenger().getLastName();
    }

    /**
     * Рассчитать сумму к оплате
     */
    default BigDecimal calculateAmountDue(Booking booking) {
        if (booking.getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal paid = booking.getPaidAmount() != null
                ? booking.getPaidAmount()
                : BigDecimal.ZERO;

        return booking.getTotalAmount().subtract(paid);
    }

    /**
     * Генерация инструкций по оплате
     */
    default String generatePaymentInstructions(Booking booking) {
        if (booking.getStatus() == BookingStatus.PENDING) {
            return "Complete payment within 15 minutes to confirm booking";
        } else if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return "Booking confirmed - payment completed";
        } else if (booking.getStatus() == BookingStatus.CANCELLED) {
            return "Booking cancelled";
        } else if (booking.getStatus() == BookingStatus.EXPIRED) {
            return "Booking expired - please create a new booking";
        } else {
            return "Booking completed";
        }
    }
}
