package com.monolit.booking.booking.mapper;

import com.monolit.booking.booking.dto.request.PassengerInfoRequest;
import com.monolit.booking.booking.dto.response.BookingDetailResponse;
import com.monolit.booking.booking.dto.response.BookingResponse;
import com.monolit.booking.booking.dto.response.PassengerResponse;
import com.monolit.booking.booking.dto.response.TicketResponse;
import com.monolit.booking.booking.entity.Booking;
import com.monolit.booking.booking.entity.Passenger;
import com.monolit.booking.booking.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(componentModel = "spring", uses = {FlightMapper.class})
public interface BookingMapper {

    BookingResponse toBookingResponse(Booking booking);

    BookingDetailResponse toBookingDetailResponse(Booking booking);

    // ═══════════════════════════════════════
    // TICKET MAPPING (ПЛОСКАЯ СТРУКТУРА)
    // ═══════════════════════════════════════

    @Mapping(target = "passengerName",
            expression = "java(getPassengerFullName(ticket.getPassenger()))")
    @Mapping(target = "flightNumber", source = "flight.flightNumber")
    @Mapping(target = "originCity", source = "flight.origin.city")
    @Mapping(target = "originIataCode", source = "flight.origin.iataCode")
    @Mapping(target = "destinationCity", source = "flight.destination.city")
    @Mapping(target = "destinationIataCode", source = "flight.destination.iataCode")
    @Mapping(target = "departureTimeUtc", source = "flight.departureTime")
    @Mapping(target = "departureTime", expression = "java(ticket.getFlight().getDepartureTimeLocal())")
    @Mapping(target = "arrivalTimeUtc", source = "flight.arrivalTime")
    @Mapping(target = "arrivalTime", expression = "java(ticket.getFlight().getArrivalTimeLocal())")
    @Mapping(target = "durationMinutes", source = "flight.durationMinutes")
    TicketResponse toTicketResponse(Ticket ticket);

    List<TicketResponse> toTicketResponseList(List<Ticket> tickets);

    // ═══════════════════════════════════════
    // PASSENGER MAPPING
    // ═══════════════════════════════════════

    PassengerResponse toPassengerResponse(Passenger passenger);

    List<PassengerResponse> toPassengerResponseList(List<Passenger> passengers);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isSaved", ignore = true)
    @Mapping(target = "passengerType", ignore = true)
    Passenger toPassenger(PassengerInfoRequest request);

    List<Passenger> toPassengerList(List<PassengerInfoRequest> requests);

    // ═══════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════

    default String getPassengerFullName(Passenger passenger) {
        if (passenger == null) {
            return null;
        }
        StringBuilder name = new StringBuilder();
        if (passenger.getFirstName() != null) {
            name.append(passenger.getFirstName());
        }
        if (passenger.getLastName() != null) {
            if (!name.isEmpty()) {
                name.append(" ");
            }
            name.append(passenger.getLastName());
        }
        return name.toString();
    }
}
