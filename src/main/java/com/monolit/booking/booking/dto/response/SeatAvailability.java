package com.monolit.booking.booking.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SeatAvailability {
    private Integer economyAvailable;
    private Integer businessAvailable;
    private Integer firstClassAvailable;

}
