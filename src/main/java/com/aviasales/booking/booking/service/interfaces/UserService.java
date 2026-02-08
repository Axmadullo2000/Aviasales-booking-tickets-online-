package com.aviasales.booking.booking.service.interfaces;

import com.aviasales.booking.booking.dto.JwtDTO;
import com.aviasales.booking.booking.dto.LoginRequest;


public interface UserService {
    void registerUser(LoginRequest dto);
    JwtDTO loginUser(LoginRequest loginRequest);
    JwtDTO refreshToken(String refreshTokenAuth);
}
