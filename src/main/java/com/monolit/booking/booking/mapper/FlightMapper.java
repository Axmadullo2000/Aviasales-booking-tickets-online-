package com.monolit.booking.booking.mapper;

import com.monolit.booking.booking.dto.response.*;
import com.monolit.booking.booking.entity.*;
import com.monolit.booking.booking.enums.SeatClass;
import org.mapstruct.*;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface FlightMapper {

    AirportResponse toAirportResponse(Airport airport);

    AirlineResponse toAirlineResponse(Airline airline);

    @Mapping(target = "price", ignore = true)
    FlightSearchResponse toFlightSearchResponse(Flight flight);

    default FlightSearchResponse toFlightSearchResponse(Flight flight, SeatClass seatClass) {
        FlightSearchResponse response = toFlightSearchResponse(flight);
        BigDecimal price = seatClass == SeatClass.BUSINESS ? flight.getPriceBusiness() : flight.getPriceEconomy();
        response.setPrice(price);
        return response;
    }

    FlightDetailResponse toFlightDetailResponse(Flight flight);

    default PopularDestinationResponse toPopularDestinationResponse(Airport airport, Long flightCount) {
        return PopularDestinationResponse.builder()
                .airport(toAirportResponse(airport))
                .flightCount(flightCount)
                .build();
    }
}
