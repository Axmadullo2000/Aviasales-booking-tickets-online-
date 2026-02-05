package com.monolit.booking.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

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

    @Column(unique = true, nullable = false, length = 2)
    private String iataCode;

    @Column(nullable = false)
    private String name;

    private String logoUrl;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @Builder.Default
    private Boolean isActive = true;
}
