package com.monolit.booking.booking.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularDestinationResponse {

    private String city;
    private String country;
    private String iataCode;
    private Integer flightCount;

}
