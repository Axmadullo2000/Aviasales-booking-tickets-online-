package com.monolit.booking.booking.dto;

import lombok.Value;

@Value
public class LoginRequest {
    String email;
    String password;
}
