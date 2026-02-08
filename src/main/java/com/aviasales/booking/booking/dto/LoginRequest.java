package com.aviasales.booking.booking.dto;

import lombok.Value;

@Value
public class LoginRequest {
    String email;
    String password;
}
