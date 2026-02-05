package com.monolit.booking.booking.mapper;

import com.monolit.booking.booking.dto.request.PassengerInfoRequest;
import com.monolit.booking.booking.dto.response.*;
import com.monolit.booking.booking.entity.*;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {FlightMapper.class})
public interface BookingMapper {

    BookingResponse toBookingResponse(Booking booking);

    @Mapping(target = "flights", source = "bookingFlights")
    BookingDetailResponse toBookingDetailResponse(Booking booking);

    @Mapping(target = "flight", ignore = true)
    BookingFlightResponse toBookingFlightResponse(BookingFlight bookingFlight);

    PassengerResponse toPassengerResponse(Passenger passenger);

    List<PassengerResponse> toPassengerResponseList(List<Passenger> passengers);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "seatNumber", ignore = true)
    Passenger toPassenger(PassengerInfoRequest request);

    List<Passenger> toPassengerList(List<PassengerInfoRequest> requests);
}
