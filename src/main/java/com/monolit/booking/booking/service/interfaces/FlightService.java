package com.monolit.booking.booking.service.interfaces;

import com.monolit.booking.booking.dto.request.CreateFlightRequest;
import com.monolit.booking.booking.dto.request.FlightSearchRequest;
import com.monolit.booking.booking.dto.request.UpdateFlightRequest;
import com.monolit.booking.booking.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FlightService {

    // ✅ Исправлено: Page<FlightResponse> вместо Page<FlightSearchResponse>
    Page<FlightResponse> searchFlights(FlightSearchRequest request, Pageable pageable);

    FlightDetailResponse getFlightById(Long id);

    FlightDetailResponse getFlightByNumber(String flightNumber);

    FlightDetailResponse createFlight(CreateFlightRequest request);

    FlightDetailResponse updateFlight(Long id, UpdateFlightRequest request);

    void deleteFlight(Long id);

    List<AirportResponse> getAllAirports();

    List<AirportResponse> searchAirports(String query);

    AirportResponse getAirportByCode(String iataCode);

    List<AirlineResponse> getAllAirlines();

    AirlineResponse getAirlineByCode(String iataCode);

    List<PopularDestinationResponse> getPopularDestinations(int limit);
}
