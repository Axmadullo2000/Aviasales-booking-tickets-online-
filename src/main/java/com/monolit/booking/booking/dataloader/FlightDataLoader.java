package com.monolit.booking.booking.dataloader;

import com.monolit.booking.booking.entity.*;
import com.monolit.booking.booking.enums.FlightStatus;
import com.monolit.booking.booking.repo.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlightDataLoader {

    private final AirportRepository airportRepository;
    private final AirlineRepository airlineRepository;
    private final FlightRepository flightRepository;

    @PostConstruct
    @Transactional
    public void loadData() {
        if (airportRepository.count() > 0) {
            log.info("Flight data already loaded, skipping initialization");
            return;
        }

        log.info("Loading flight test data...");
        List<Airport> airports = loadAirports();
        List<Airline> airlines = loadAirlines();
        loadFlights(airports, airlines);
        log.info("Flight test data loaded successfully");
    }

    private List<Airport> loadAirports() {
        List<Airport> airports = Arrays.asList(
                Airport.builder()
                        .iataCode("TAS")
                        .name("Tashkent International Airport")
                        .city("Tashkent")
                        .country("Uzbekistan")
                        .timezone("Asia/Tashkent")
                        .latitude(41.2573)
                        .longitude(69.2817)
                        .isActive(true)
                        .build(),

                Airport.builder()
                        .iataCode("SVO")
                        .name("Sheremetyevo International Airport")
                        .city("Moscow")
                        .country("Russia")
                        .timezone("Europe/Moscow")
                        .latitude(55.9726)
                        .longitude(37.4146)
                        .isActive(true)
                        .build(),

                Airport.builder()
                        .iataCode("LED")
                        .name("Pulkovo Airport")
                        .city("Saint Petersburg")
                        .country("Russia")
                        .timezone("Europe/Moscow")
                        .latitude(59.8003)
                        .longitude(30.2625)
                        .isActive(true)
                        .build(),

                Airport.builder()
                        .iataCode("DXB")
                        .name("Dubai International Airport")
                        .city("Dubai")
                        .country("United Arab Emirates")
                        .timezone("Asia/Dubai")
                        .latitude(25.2532)
                        .longitude(55.3657)
                        .isActive(true)
                        .build(),

                Airport.builder()
                        .iataCode("IST")
                        .name("Istanbul Airport")
                        .city("Istanbul")
                        .country("Turkey")
                        .timezone("Europe/Istanbul")
                        .latitude(41.2753)
                        .longitude(28.7519)
                        .isActive(true)
                        .build(),

                Airport.builder()
                        .iataCode("SKD")
                        .name("Samarkand International Airport")
                        .city("Samarkand")
                        .country("Uzbekistan")
                        .timezone("Asia/Tashkent")
                        .latitude(39.7005)
                        .longitude(66.9838)
                        .isActive(true)
                        .build(),

                Airport.builder()
                        .iataCode("BHK")
                        .name("Bukhara International Airport")
                        .city("Bukhara")
                        .country("Uzbekistan")
                        .timezone("Asia/Tashkent")
                        .latitude(39.7750)
                        .longitude(64.4833)
                        .isActive(true)
                        .build(),

                Airport.builder()
                        .iataCode("FRU")
                        .name("Manas International Airport")
                        .city("Bishkek")
                        .country("Kyrgyzstan")
                        .timezone("Asia/Bishkek")
                        .latitude(43.0613)
                        .longitude(74.4776)
                        .isActive(true)
                        .build()
        );

        airports = airportRepository.saveAll(airports);
        log.info("Loaded {} airports", airports.size());
        return airports;
    }

    private List<Airline> loadAirlines() {
        List<Airline> airlines = Arrays.asList(
                Airline.builder()
                        .iataCode("HY")
                        .name("Uzbekistan Airways")
                        .logoUrl("https://example.com/logos/hy.png")
                        .rating(new BigDecimal("4.2"))
                        .isActive(true)
                        .build(),

                Airline.builder()
                        .iataCode("SU")
                        .name("Aeroflot")
                        .logoUrl("https://example.com/logos/su.png")
                        .rating(new BigDecimal("4.0"))
                        .isActive(true)
                        .build(),

                Airline.builder()
                        .iataCode("TK")
                        .name("Turkish Airlines")
                        .logoUrl("https://example.com/logos/tk.png")
                        .rating(new BigDecimal("4.3"))
                        .isActive(true)
                        .build(),

                Airline.builder()
                        .iataCode("EK")
                        .name("Emirates")
                        .logoUrl("https://example.com/logos/ek.png")
                        .rating(new BigDecimal("4.5"))
                        .isActive(true)
                        .build(),

                Airline.builder()
                        .iataCode("QR")
                        .name("Qatar Airways")
                        .logoUrl("https://example.com/logos/qr.png")
                        .rating(new BigDecimal("4.4"))
                        .isActive(true)
                        .build()
        );

        airlines = airlineRepository.saveAll(airlines);
        log.info("Loaded {} airlines", airlines.size());
        return airlines;
    }

    private void loadFlights(List<Airport> airports, List<Airline> airlines) {
        Map<String, Airport> airportMap = new HashMap<>();
        for (Airport airport : airports) {
            airportMap.put(airport.getIataCode(), airport);
        }

        Map<String, Airline> airlineMap = new HashMap<>();
        for (Airline airline : airlines) {
            airlineMap.put(airline.getIataCode(), airline);
        }

        List<Flight> flights = new ArrayList<>();

        // Используем OffsetDateTime с конкретной timezone (например, Asia/Tashkent для Ташкента)
        OffsetDateTime baseTime = OffsetDateTime.now(ZoneId.of("Asia/Tashkent"))
                .plusHours(2)
                .truncatedTo(ChronoUnit.HOURS);

        // Рейсы Ташкент - Москва
        flights.add(createFlight("HY501", airlineMap.get("HY"),
                airportMap.get("TAS"), airportMap.get("SVO"),
                baseTime, 240, 180,
                new BigDecimal("250"), new BigDecimal("650")));

        flights.add(createFlight("HY503", airlineMap.get("HY"),
                airportMap.get("TAS"), airportMap.get("SVO"),
                baseTime.plusDays(1).withHour(8), 245, 180,
                new BigDecimal("230"), new BigDecimal("600")));

        flights.add(createFlight("HY505", airlineMap.get("HY"),
                airportMap.get("TAS"), airportMap.get("SVO"),
                baseTime.plusDays(2).withHour(14), 240, 180,
                new BigDecimal("260"), new BigDecimal("680")));

        // Рейсы Москва - Ташкент
        flights.add(createFlight("SU1876", airlineMap.get("SU"),
                airportMap.get("SVO"), airportMap.get("TAS"),
                baseTime.plusHours(6), 255, 200,
                new BigDecimal("280"), new BigDecimal("720")));

        flights.add(createFlight("SU1878", airlineMap.get("SU"),
                airportMap.get("SVO"), airportMap.get("TAS"),
                baseTime.plusDays(1).withHour(10), 250, 200,
                new BigDecimal("265"), new BigDecimal("690")));

        // Рейсы Ташкент - Дубай
        flights.add(createFlight("HY601", airlineMap.get("HY"),
                airportMap.get("TAS"), airportMap.get("DXB"),
                baseTime.plusHours(3), 195, 180,
                new BigDecimal("320"), new BigDecimal("850")));

        flights.add(createFlight("EK378", airlineMap.get("EK"),
                airportMap.get("DXB"), airportMap.get("TAS"),
                baseTime.plusDays(1).withHour(2), 200, 250,
                new BigDecimal("350"), new BigDecimal("950")));

        // Рейсы Ташкент - Стамбул
        flights.add(createFlight("TK367", airlineMap.get("TK"),
                airportMap.get("TAS"), airportMap.get("IST"),
                baseTime.plusHours(4), 330, 220,
                new BigDecimal("380"), new BigDecimal("1100")));

        flights.add(createFlight("TK366", airlineMap.get("TK"),
                airportMap.get("IST"), airportMap.get("TAS"),
                baseTime.plusDays(1).withHour(6), 335, 220,
                new BigDecimal("395"), new BigDecimal("1150")));

        // Рейс Ташкент - Санкт-Петербург
        flights.add(createFlight("HY701", airlineMap.get("HY"),
                airportMap.get("TAS"), airportMap.get("LED"),
                baseTime.plusDays(2).withHour(7), 285, 160,
                new BigDecimal("290"), new BigDecimal("750")));

        // Внутренние рейсы Узбекистана
        flights.add(createFlight("HY55", airlineMap.get("HY"),
                airportMap.get("TAS"), airportMap.get("SKD"),
                baseTime.plusHours(1), 55, 120,
                new BigDecimal("45"), new BigDecimal("120")));

        flights.add(createFlight("HY57", airlineMap.get("HY"),
                airportMap.get("TAS"), airportMap.get("BHK"),
                baseTime.plusDays(1).withHour(16), 65, 120,
                new BigDecimal("50"), new BigDecimal("130")));

        flights.add(createFlight("HY61", airlineMap.get("HY"),
                airportMap.get("TAS"), airportMap.get("FRU"),
                baseTime.plusHours(5), 70, 100,
                new BigDecimal("85"), new BigDecimal("220")));

        // Дополнительные рейсы на неделю
        for (int i = 0; i < 7; i++) {
            flights.add(createFlight("HY50" + (7 + i), airlineMap.get("HY"),
                    airportMap.get("TAS"), airportMap.get("SVO"),
                    baseTime.plusDays(3 + i).withHour(9), 245, 180,
                    new BigDecimal("240"), new BigDecimal("620")));
        }

        // Ночные рейсы
        flights.add(createFlight("TK368", airlineMap.get("TK"),
                airportMap.get("TAS"), airportMap.get("IST"),
                baseTime.plusDays(3).withHour(23), 325, 220,
                new BigDecimal("360"), new BigDecimal("1050")));

        flights.add(createFlight("EK380", airlineMap.get("EK"),
                airportMap.get("DXB"), airportMap.get("TAS"),
                baseTime.plusDays(4).withHour(4), 195, 280,
                new BigDecimal("340"), new BigDecimal("920")));

        flights.add(createFlight("SU1880", airlineMap.get("SU"),
                airportMap.get("SVO"), airportMap.get("TAS"),
                baseTime.plusDays(5).withHour(12), 250, 200,
                new BigDecimal("275"), new BigDecimal("710")));

        // Транзитные рейсы
        flights.add(createFlight("QR340", airlineMap.get("QR"),
                airportMap.get("DXB"), airportMap.get("IST"),
                baseTime.plusDays(2).withHour(8), 240, 300,
                new BigDecimal("420"), new BigDecimal("1200")));

        flights.add(createFlight("TK180", airlineMap.get("TK"),
                airportMap.get("IST"), airportMap.get("SVO"),
                baseTime.plusDays(3).withHour(11), 165, 200,
                new BigDecimal("180"), new BigDecimal("480")));

        // Задержанный рейс
        Flight delayedFlight = createFlight("HY999", airlineMap.get("HY"),
                airportMap.get("TAS"), airportMap.get("SVO"),
                baseTime.plusDays(1).withHour(20), 250, 180,
                new BigDecimal("220"), new BigDecimal("580"));
        delayedFlight.setStatus(FlightStatus.DELAYED);
        flights.add(delayedFlight);

        flightRepository.saveAll(flights);
        log.info("Loaded {} flights", flights.size());
    }

    private Flight createFlight(String flightNumber, Airline airline,
                                Airport departure, Airport arrival,
                                OffsetDateTime departureTime, int durationMinutes,
                                int totalSeats, BigDecimal priceEconomy,
                                BigDecimal priceBusiness) {
        return Flight.builder()
                .flightNumber(flightNumber)
                .airline(airline)
                .departureAirport(departure)
                .arrivalAirport(arrival)
                .departureTime(departureTime)
                .arrivalTime(departureTime.plusMinutes(durationMinutes))
                .durationMinutes(durationMinutes)
                .totalSeats(totalSeats)
                .availableSeats(totalSeats)
                .priceEconomy(priceEconomy)
                .priceBusiness(priceBusiness)
                .status(FlightStatus.SCHEDULED)
                .build();
    }
}