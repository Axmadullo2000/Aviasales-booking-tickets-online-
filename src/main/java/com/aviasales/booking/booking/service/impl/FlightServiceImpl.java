package com.aviasales.booking.booking.service.impl;

import com.aviasales.booking.booking.dto.request.CreateFlightRequest;
import com.aviasales.booking.booking.dto.request.FlightSearchRequest;
import com.aviasales.booking.booking.dto.request.UpdateFlightRequest;
import com.aviasales.booking.booking.dto.response.*;
import com.aviasales.booking.booking.entity.Airline;
import com.aviasales.booking.booking.entity.Airport;
import com.aviasales.booking.booking.entity.Flight;
import com.aviasales.booking.booking.enums.CabinClass;
import com.aviasales.booking.booking.enums.FlightSortBy;
import com.aviasales.booking.booking.enums.FlightStatus;
import com.aviasales.booking.booking.exception.AirlineNotFoundException;
import com.aviasales.booking.booking.exception.AirportNotFoundException;
import com.aviasales.booking.booking.exception.FlightNotFoundException;
import com.aviasales.booking.booking.mapper.FlightMapper;
import com.aviasales.booking.booking.repo.AirlineRepository;
import com.aviasales.booking.booking.repo.AirportRepository;
import com.aviasales.booking.booking.repo.FlightRepository;
import com.aviasales.booking.booking.service.interfaces.FlightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;
    private final AirlineRepository airlineRepository;
    private final FlightMapper flightMapper;

    // ═══════════════════════════════════════
    // ПОИСК РЕЙСОВ
    // ═══════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Page<FlightResponse> searchFlights(FlightSearchRequest request, Pageable pageable) {
        log.info("Searching flights from {} to {} on {}, passengers: {}",
                request.getOriginCode(), request.getDestinationCode(),
                request.getDepartureDate(), request.getPassengers());

        // Конвертируем LocalDate в Instant (начало и конец дня в UTC)
        Instant startDate = request.getDepartureDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        Instant endDate = request.getDepartureDate()
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        // Создаём сортировку
        Sort sort = createSort(request.getSortBy(), request.getCabinClass());
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );

        // Ищем рейсы
        Page<Flight> flights = flightRepository.searchFlights(
                request.getOriginCode().toUpperCase(),
                request.getDestinationCode().toUpperCase(),
                startDate,
                endDate,
                request.getPassengers(),
                FlightStatus.SCHEDULED,
                sortedPageable
        );

        log.info("Found {} flights", flights.getTotalElements());

        return flights.map(flightMapper::toFlightResponse);
    }

    /**
     * Создать сортировку по критерию
     */
    private Sort createSort(FlightSortBy sortBy, CabinClass cabinClass) {
        if (sortBy == null) {
            sortBy = FlightSortBy.PRICE;
        }

        return switch (sortBy) {
            case TIME -> Sort.by(Sort.Direction.ASC, "departureTime");
            case DURATION -> Sort.by(Sort.Direction.ASC, "durationMinutes");
            case PRICE -> {
                String priceField = (cabinClass == CabinClass.BUSINESS || cabinClass == CabinClass.FIRST_CLASS)
                        ? "businessPrice"
                        : "basePrice";
                yield Sort.by(Sort.Direction.ASC, priceField);
            }
        };
    }

    // ═══════════════════════════════════════
    // ПОЛУЧЕНИЕ РЕЙСА
    // ═══════════════════════════════════════

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
        Flight flight = flightRepository.findByFlightNumber(flightNumber.toUpperCase())
                .orElseThrow(() -> new FlightNotFoundException(
                        "Flight not found with number: " + flightNumber
                ));
        return flightMapper.toFlightDetailResponse(flight);
    }

    // ═══════════════════════════════════════
    // СОЗДАНИЕ РЕЙСА
    // ═══════════════════════════════════════

    @Override
    @Transactional
    public FlightDetailResponse createFlight(CreateFlightRequest request) {
        log.info("Creating flight: {}", request.getFlightNumber());

        // Валидация времени
        if (request.getDepartureTime().isAfter(request.getArrivalTime())) {
            throw new IllegalArgumentException("Arrival time must be after departure time");
        }

        // Проверка существования рейса с таким номером
        if (flightRepository.existsByFlightNumber(request.getFlightNumber().toUpperCase())) {
            throw new IllegalArgumentException(
                    "Flight with number " + request.getFlightNumber() + " already exists"
            );
        }

        boolean exists = flightRepository.existsByDepartureTimeLessThanAndArrivalTimeGreaterThan(
                request.getArrivalTime(), request.getDepartureTime()
        );

        if (exists) {
            throw new IllegalArgumentException(
                    "Flight to this date (%s - %s) has been already registered!".formatted(
                            request.getDepartureTime(), request.getArrivalTime())
            );
        }
        // Получаем авиакомпанию
        Airline airline = airlineRepository.findByIataCode(request.getAirlineCode().toUpperCase())
                .orElseThrow(() -> new AirlineNotFoundException(request.getAirlineCode()));

        // Получаем аэропорты
        Airport origin = airportRepository.findByIataCode(request.getOriginCode().toUpperCase())
                .orElseThrow(() -> new AirportNotFoundException(request.getOriginCode()));

        Airport destination = airportRepository.findByIataCode(request.getDestinationCode().toUpperCase())
                .orElseThrow(() -> new AirportNotFoundException(request.getDestinationCode()));

        // Создаём рейс
        Flight flight = Flight.builder()
                .flightNumber(request.getFlightNumber().toUpperCase())
                .airline(airline)
                .origin(origin)
                .destination(destination)
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .economySeats(request.getEconomySeats())
                .businessSeats(request.getBusinessSeats())
                .firstClassSeats(request.getFirstClassSeats())
                // ✅ ЯВНО УСТАНАВЛИВАЕМ available равными seats
                .availableEconomy(request.getEconomySeats() != null ? request.getEconomySeats() : 0)
                .availableBusiness(request.getBusinessSeats() != null ? request.getBusinessSeats() : 0)
                .availableFirstClass(request.getFirstClassSeats() != null ? request.getFirstClassSeats() : 0)
                .basePrice(request.getBasePrice())
                .businessPrice(request.getBusinessPrice())
                .firstClassPrice(request.getFirstClassPrice())
                .aircraftType(request.getAircraftType())
                .stops(0)
                .status(FlightStatus.SCHEDULED)
                .build();

        flight = flightRepository.save(flight);

        flight = flightRepository.save(flight);
        log.info("Flight created with id: {}", flight.getId());

        return flightMapper.toFlightDetailResponse(flight);
    }

    // ═══════════════════════════════════════
    // ОБНОВЛЕНИЕ РЕЙСА
    // ═══════════════════════════════════════

    @Override
    @Transactional
    @CacheEvict(value = "flights", key = "#id")
    public FlightDetailResponse updateFlight(Long id, UpdateFlightRequest request) {
        log.info("Updating flight: {}", id);

        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new FlightNotFoundException(id));

        // Обновляем время
        if (request.getDepartureTime() != null) {
            flight.setDepartureTime(request.getDepartureTime());
        }
        if (request.getArrivalTime() != null) {
            flight.setArrivalTime(request.getArrivalTime());
        }

        // Обновляем места
        if (request.getTotalSeats() != null) {
            int seatDifference = request.getTotalSeats() - flight.getTotalSeats();
            flight.setTotalSeats(request.getTotalSeats());
            flight.setAvailableSeats(Math.max(0, flight.getAvailableSeats() + seatDifference));
        }

        // Обновляем цены
        if (request.getBasePrice() != null) {
            flight.setBasePrice(request.getBasePrice());
        }
        if (request.getBusinessPrice() != null) {
            flight.setBusinessPrice(request.getBusinessPrice());
        }
        if (request.getFirstClassPrice() != null) {
            flight.setFirstClassPrice(request.getFirstClassPrice());
        }

        // Обновляем статус
        if (request.getStatus() != null) {
            flight.setStatus(request.getStatus());
        }

        flight = flightRepository.save(flight);
        log.info("Flight updated: {}", id);

        return flightMapper.toFlightDetailResponse(flight);  // ✅ добавил return
    }

    // ═══════════════════════════════════════
    // УДАЛЕНИЕ РЕЙСА
    // ═══════════════════════════════════════

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

    // ═══════════════════════════════════════
    // АЭРОПОРТЫ
    // ═══════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "airports")
    public List<AirportResponse> getAllAirports() {
        log.info("Getting all active airports");
        return airportRepository.findByIsActiveTrue()
                .stream()
                .map(flightMapper::toAirportResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AirportResponse> searchAirports(String query) {
        log.info("Searching airports with query: {}", query);
        return airportRepository.searchAirports(query)
                .stream()
                .map(flightMapper::toAirportResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "airports", key = "#iataCode")
    public AirportResponse getAirportByCode(String iataCode) {
        log.info("Getting airport by code: {}", iataCode);
        Airport airport = airportRepository.findByIataCode(iataCode.toUpperCase())
                .orElseThrow(() -> new AirportNotFoundException(iataCode));
        return flightMapper.toAirportResponse(airport);
    }

    // ═══════════════════════════════════════
    // АВИАКОМПАНИИ
    // ═══════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "airlines")
    public List<AirlineResponse> getAllAirlines() {
        log.info("Getting all active airlines");
        return airlineRepository.findByIsActiveTrue()
                .stream()
                .map(flightMapper::toAirlineResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "airlines", key = "#iataCode")
    public AirlineResponse getAirlineByCode(String iataCode) {
        log.info("Getting airline by code: {}", iataCode);
        Airline airline = airlineRepository.findByIataCode(iataCode.toUpperCase())
                .orElseThrow(() -> new AirlineNotFoundException(iataCode));
        return flightMapper.toAirlineResponse(airline);
    }

    // ═══════════════════════════════════════
    // ПОПУЛЯРНЫЕ НАПРАВЛЕНИЯ
    // ═══════════════════════════════════════

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

                    return PopularDestinationResponse.builder()
                            .city(airport.getCity())
                            .country(airport.getCountry())
                            .iataCode(airport.getIataCode())
                            .flightCount(count.intValue())
                            .build();
                })
                .toList();
    }

    @Override
    public Flight findById(Long flightId) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found: " + flightId));
    }
}
