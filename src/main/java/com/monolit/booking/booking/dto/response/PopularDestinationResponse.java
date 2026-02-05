package com.monolit.booking.booking.dto.response;

import lombok.*;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularDestinationResponse implements Serializable {

    private AirportResponse airport;
    private Long flightCount;
}
