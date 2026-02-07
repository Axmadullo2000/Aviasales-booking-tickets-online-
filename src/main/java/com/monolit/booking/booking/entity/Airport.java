package com.monolit.booking.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "airports", indexes = {
        @Index(name = "idx_iata_code", columnList = "iataCode", unique = true),
        @Index(name = "idx_city_country", columnList = "city, country")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Airport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 3)
    private String iataCode;  // DME, JFK, DXB

    @Column(unique = true, length = 4)
    private String icaoCode;  // UUDD, KJFK

    @Column(nullable = false)
    private String name;  // Домодедово

    @Column(nullable = false)
    private String city;  // ✅ Москва

    @Column(nullable = false)
    private String country;  // ✅ Россия

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false)
    private String timezone;  // Europe/Moscow

    private Boolean isActive;  // ✅ должно быть isActive

    @Column(columnDefinition = "TEXT")
    private String searchTokens;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
