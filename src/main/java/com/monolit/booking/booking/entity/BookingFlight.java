package com.monolit.booking.booking.entity;

import com.monolit.booking.booking.enums.SeatClass;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "booking_flights")
public class BookingFlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private Long flightId;

    @Column(nullable = false)
    private Integer passengerCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatClass seatClass;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerSeat;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
}
