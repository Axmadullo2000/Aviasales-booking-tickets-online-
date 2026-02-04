package com.monolit.booking.booking.service.interfaces;

import com.monolit.booking.booking.dto.JwtDTO;
import com.monolit.booking.booking.dto.LoginRequest;


public interface UserService {
    void registerUser(LoginRequest dto);
    JwtDTO loginUser(LoginRequest loginRequest);
    JwtDTO refreshToken(String refreshTokenAuth);
}
