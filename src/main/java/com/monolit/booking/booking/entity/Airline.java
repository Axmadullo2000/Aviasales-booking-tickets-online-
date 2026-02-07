package com.monolit.booking.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "airlines")
public class Airline implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 2)
    private String iataCode;

    @Column(unique = true, length = 3)
    private String icaoCode;

    @Column(nullable = false)
    private String name;

    private String nameEng;

    private String country;
    private String logoUrl;

    private Integer baggageAllowance;
    private Integer handLuggage;

    @Column(columnDefinition = "TEXT")
    private String refundPolicy;

    private Boolean isLowCost;
    @Builder.Default
    private Boolean isActive = true;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @CreatedDate
    private Instant createdAt;

}
