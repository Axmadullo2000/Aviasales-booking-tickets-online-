package com.monolit.booking.booking.entity;

import com.monolit.booking.booking.enums.FlightStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(
        name = "flights",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_flight_number_departure_time",
                        columnNames = {"flight_number", "departure_time"}
                )
        }
)
public class Flight implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String flightNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "airline_id", nullable = false)
    private Airline airline;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "departure_airport_id", nullable = false)
    private Airport departureAirport;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "arrival_airport_id", nullable = false)
    private Airport arrivalAirport;

    @Column(nullable = false)
    private OffsetDateTime departureTime;

    @Column(nullable = false)
    private OffsetDateTime arrivalTime;

    private Integer durationMinutes;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(nullable = false)
    private Integer availableSeats;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceEconomy;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceBusiness;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FlightStatus status = FlightStatus.SCHEDULED;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    public synchronized boolean reserveSeats(int count) {
        if (availableSeats >= count) {
            availableSeats -= count;
            return true;
        }
        return false;
    }

    public synchronized void releaseSeats(int count) {
        availableSeats = Math.min(availableSeats + count, totalSeats);
    }

    public boolean hasAvailableSeats(int count) {
        return availableSeats >= count;
    }
}
