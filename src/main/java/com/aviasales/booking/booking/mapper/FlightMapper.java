package com.aviasales.booking.booking.mapper;

import com.aviasales.booking.booking.dto.response.*;
import com.aviasales.booking.booking.entity.Airline;
import com.aviasales.booking.booking.entity.Airport;
import com.aviasales.booking.booking.entity.Flight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FlightMapper {

    @Mapping(target = "economyPrice", source = "basePrice")
    @Mapping(target = "departureTimeUtc", source = "departureTime")
    @Mapping(target = "arrivalTimeUtc", source = "arrivalTime")
    @Mapping(target = "departureTime", expression = "java(flight.getDepartureTimeLocal())")
    @Mapping(target = "arrivalTime", expression = "java(flight.getArrivalTimeLocal())")
    FlightResponse toFlightResponse(Flight flight);

    List<FlightResponse> toFlightResponseList(List<Flight> flights);

    @Mapping(target = "flight", source = ".")
    @Mapping(target = "seatAvailability", expression = "java(mapSeatAvailability(flight))")
    @Mapping(target = "connectionAirport", source = "connectionAirport")
    FlightDetailResponse toFlightDetailResponse(Flight flight);

    AirportResponse toAirportResponse(Airport airport);

    List<AirportResponse> toAirportResponseList(List<Airport> airports);

    AirlineResponse toAirlineResponse(Airline airline);

    List<AirlineResponse> toAirlineResponseList(List<Airline> airlines);

    default SeatAvailability mapSeatAvailability(Flight flight) {
        if (flight.getEconomySeats() == null || flight.getBusinessSeats() == null) {
            return null;
        }

        int totalOccupied = flight.getTotalSeats() - flight.getAvailableSeats();

        int economyOccupied = totalOccupied * flight.getEconomySeats() / flight.getTotalSeats();
        int businessOccupied = totalOccupied * flight.getBusinessSeats() / flight.getTotalSeats();
        int firstClassOccupied = totalOccupied - economyOccupied - businessOccupied;

        return SeatAvailability.builder()
                .economyAvailable(Math.max(0, flight.getEconomySeats() - economyOccupied))
                .businessAvailable(Math.max(0, flight.getBusinessSeats() - businessOccupied))
                .firstClassAvailable(Math.max(0,
                        (flight.getFirstClassSeats() != null ? flight.getFirstClassSeats() : 0) - firstClassOccupied
                ))
                .build();
    }
}
