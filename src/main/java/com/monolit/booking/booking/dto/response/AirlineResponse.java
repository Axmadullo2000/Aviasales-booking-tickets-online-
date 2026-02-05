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
    private String iataCode;
    private String name;
    private String logoUrl;
    private BigDecimal rating;
}
