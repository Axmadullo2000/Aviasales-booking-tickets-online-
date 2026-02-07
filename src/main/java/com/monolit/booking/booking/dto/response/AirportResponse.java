package com.monolit.booking.booking.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirportResponse {

    private Long id;

    private String iataCode;   // DME, JFK, DXB
    private String name;       // Домодедово
    private String city;       // Москва
    private String country;    // Россия

    // ✅ КРИТИЧЕСКИ ВАЖНО для международных рейсов
    private String timezone;   // Europe/Moscow, America/New_York, Asia/Dubai

    // Координаты (опционально, для карты)
    private BigDecimal latitude;
    private BigDecimal longitude;
}
