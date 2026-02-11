package com.aviasales.booking.booking.controller;


import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    @GetMapping
    @PreAuthorize("hasRole('ROLE_USER')")
    public HttpEntity<?> userMenu() {
        return ResponseEntity.ok("Welcome to User menu!");
    }
}
