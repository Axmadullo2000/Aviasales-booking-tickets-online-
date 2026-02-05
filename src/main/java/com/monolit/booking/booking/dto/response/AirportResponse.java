package com.monolit.booking.booking.dto.response;

import lombok.*;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirportResponse implements Serializable {

    private Long id;
    private String iataCode;
    private String name;
    private String city;
    private String country;
    private String timezone;
}
