package com.aviasales.booking.booking.dto;

import lombok.Value;

@Value
public class JwtDTO {
    String accessToken;
    String refreshToken;
}
