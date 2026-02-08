package com.aviasales.booking.booking.controller;


import com.aviasales.booking.booking.dto.JwtDTO;
import com.aviasales.booking.booking.dto.LoginRequest;
import com.aviasales.booking.booking.service.impl.UserServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserServiceImpl userServiceImpl;

    @PostMapping("/login")
    public ResponseEntity<JwtDTO> login(@RequestBody LoginRequest loginRequest) {
        log.info("Login request: {}", loginRequest);
        JwtDTO jwtDTO = userServiceImpl.loginUser(loginRequest);
        return ResponseEntity.ok(jwtDTO);
    }

    @GetMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @RequestHeader("refreshTokenAuth") String refreshTokenAuth) {
        JwtDTO jwtDTO = userServiceImpl.refreshToken(refreshTokenAuth);
        return ResponseEntity.ok(jwtDTO);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest dto) {
        log.info("Register request: {}", dto);

        userServiceImpl.registerUser(dto);
        return ResponseEntity.ok("User registered successfully");
    }

}
