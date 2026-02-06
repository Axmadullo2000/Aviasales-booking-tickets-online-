package com.monolit.booking.booking.service.impl;

import com.monolit.booking.booking.dto.request.*;
import com.monolit.booking.booking.dto.response.*;
import com.monolit.booking.booking.entity.*;
import com.monolit.booking.booking.enums.*;
import com.monolit.booking.booking.exception.*;
import com.monolit.booking.booking.mapper.FlightMapper;
import com.monolit.booking.booking.repo.*;
import com.monolit.booking.booking.service.interfaces.FlightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;
    private final AirlineRepository airlineRepository;
    private final FlightMapper flightMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<FlightSearchResponse> searchFlights(FlightSearchRequest request, Pageable pageable) {
        log.info("Searching flights from {} to {} on {}",
                request.getDepartureAirport(), request.getArrivalAirport(), request.getDepartureDate());

        Instant startDate = request.getDepartureDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endDate = request.getDepartureDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Sort sort = createSort(request.getSortBy(), request.getSeatClass());
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<Flight> flights = flightRepository.searchFlights(
                request.getDepartureAirport().toUpperCase(),
                request.getArrivalAirport().toUpperCase(),
                startDate,
                endDate,
                request.getPassengers(),
                FlightStatus.SCHEDULED,
                sortedPageable
        );

        SeatClass seatClass = request.getSeatClass() != null ? request.getSeatClass() : SeatClass.ECONOMY;

        return flights.map(flight -> flightMapper.toFlightSearchResponse(flight, seatClass));
    }

    private Sort createSort(FlightSortBy sortBy, SeatClass seatClass) {
        if (sortBy == null) {
            sortBy = FlightSortBy.PRICE;
        }

        return switch (sortBy) {
            case TIME -> Sort.by(Sort.Direction.ASC, "departureTime");
            case DURATION -> Sort.by(Sort.Direction.ASC, "durationMinutes");
            case PRICE -> {
                String priceField = seatClass == SeatClass.BUSINESS ? "priceBusiness" : "priceEconomy";
                yield Sort.by(Sort.Direction.ASC, priceField);
            }
        };
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "flights", key = "#id")
    public FlightDetailResponse getFlightById(Long id) {
        log.info("Getting flight by id: {}", id);
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new FlightNotFoundException(id));
        return flightMapper.toFlightDetailResponse(flight);
    }

    @Override
    @Transactional(readOnly = true)
    public FlightDetailResponse getFlightByNumber(String flightNumber) {
        log.info("Getting flight by number: {}", flightNumber);
        Flight flight = flightRepository.findByFlightNumber(flightNumber)
                .orElseThrow(() -> new FlightNotFoundException("Flight not found with number: " + flightNumber));
        return flightMapper.toFlightDetailResponse(flight);
    }

    @Override
    @Transactional
    public FlightDetailResponse createFlight(CreateFlightRequest request) {
        log.info("Creating flight: {}", request.getFlightNumber());

        if (request.getDepartureTime().isAfter(request.getArrivalTime())) throw new IllegalArgumentException("Departure time should be after arrival time");

        Airline airline = airlineRepository.findByIataCode(request.getAirlineCode().toUpperCase())
                .orElseThrow(() -> new AirlineNotFoundException(request.getAirlineCode(), true));

        Airport departureAirport = airportRepository.findByIataCode(request.getDepartureAirportCode().toUpperCase())
                .orElseThrow(() -> new AirportNotFoundException(request.getDepartureAirportCode(), true));

        Airport arrivalAirport = airportRepository.findByIataCode(request.getArrivalAirportCode().toUpperCase())
                .orElseThrow(() -> new AirportNotFoundException(request.getArrivalAirportCode(), true));

        int durationMinutes = (int) Duration.between(request.getDepartureTime(), request.getArrivalTime()).toMinutes();

        Flight flight = Flight.builder()
                .flightNumber(request.getFlightNumber())
                .airline(airline)
                .departureAirport(departureAirport)
                .arrivalAirport(arrivalAirport)
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .durationMinutes(durationMinutes)
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .priceEconomy(request.getPriceEconomy())
                .priceBusiness(request.getPriceBusiness())
                .status(FlightStatus.SCHEDULED)
                .build();

        flight = flightRepository.save(flight);
        log.info("Flight created with id: {}", flight.getId());

        return flightMapper.toFlightDetailResponse(flight);
    }

    @Override
    @Transactional
    @CacheEvict(value = "flights", key = "#id")
    public FlightDetailResponse updateFlight(Long id, UpdateFlightRequest request) {
        log.info("Updating flight: {}", id);

        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new FlightNotFoundException(id));

        if (request.getDepartureTime() != null) {
            flight.setDepartureTime(request.getDepartureTime());
        }
        if (request.getArrivalTime() != null) {
            flight.setArrivalTime(request.getArrivalTime());
        }
        if (request.getDepartureTime() != null || request.getArrivalTime() != null) {
            int durationMinutes = (int) Duration.between(flight.getDepartureTime(), flight.getArrivalTime()).toMinutes();
            flight.setDurationMinutes(durationMinutes);
        }
        if (request.getTotalSeats() != null) {
            int seatDifference = request.getTotalSeats() - flight.getTotalSeats();
            flight.setTotalSeats(request.getTotalSeats());
            flight.setAvailableSeats(Math.max(0, flight.getAvailableSeats() + seatDifference));
        }
        if (request.getPriceEconomy() != null) {
            flight.setPriceEconomy(request.getPriceEconomy());
        }
        if (request.getPriceBusiness() != null) {
            flight.setPriceBusiness(request.getPriceBusiness());
        }
        if (request.getStatus() != null) {
            flight.setStatus(request.getStatus());
        }

        flight = flightRepository.save(flight);
        log.info("Flight updated: {}", id);

        return flightMapper.toFlightDetailResponse(flight);
    }

    @Override
    @Transactional
    @CacheEvict(value = "flights", key = "#id")
    public void deleteFlight(Long id) {
        log.info("Deleting flight: {}", id);
        if (!flightRepository.existsById(id)) {
            throw new FlightNotFoundException(id);
        }
        flightRepository.deleteById(id);
        log.info("Flight deleted: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "airports")
    public List<AirportResponse> getAllAirports() {
        log.info("Getting all active airports");
        return airportRepository.findByIsActiveTrue()
                .stream()
                .map(flightMapper::toAirportResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AirportResponse> searchAirports(String query) {
        log.info("Searching airports with query: {}", query);
        return airportRepository.searchAirports(query)
                .stream()
                .map(flightMapper::toAirportResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "airports", key = "#iataCode")
    public AirportResponse getAirportByCode(String iataCode) {
        log.info("Getting airport by code: {}", iataCode);
        Airport airport = airportRepository.findByIataCode(iataCode.toUpperCase())
                .orElseThrow(() -> new AirportNotFoundException(iataCode, true));
        return flightMapper.toAirportResponse(airport);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "airlines")
    public List<AirlineResponse> getAllAirlines() {
        log.info("Getting all active airlines");
        return airlineRepository.findByIsActiveTrue()
                .stream()
                .map(flightMapper::toAirlineResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "airlines", key = "#iataCode")
    public AirlineResponse getAirlineByCode(String iataCode) {
        log.info("Getting airline by code: {}", iataCode);
        Airline airline = airlineRepository.findByIataCode(iataCode.toUpperCase())
                .orElseThrow(() -> new AirlineNotFoundException(iataCode, true));
        return flightMapper.toAirlineResponse(airline);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PopularDestinationResponse> getPopularDestinations(int limit) {
        log.info("Getting top {} popular destinations", limit);
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> results = flightRepository.findPopularDestinations(pageable);

        return results.stream()
                .map(row -> {
                    Airport airport = (Airport) row[0];
                    Long count = (Long) row[1];
                    return flightMapper.toPopularDestinationResponse(airport, count);
                })
                .collect(Collectors.toList());
    }
}
