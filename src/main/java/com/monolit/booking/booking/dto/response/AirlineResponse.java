package com.monolit.booking.booking.dto.response;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirlineResponse implements Serializable {

    private Long id;
    private String iataCode;   // SU, EK
    private String name;       // Аэрофлот
    private String logoUrl;    // URL логотипа для UI

    private Integer baggageAllowance;  // кг бесплатного багажа
    private Integer handLuggage; // кг ручной клади
    private Boolean isLowCost; // лоукостер или нет


    private BigDecimal rating;
}
