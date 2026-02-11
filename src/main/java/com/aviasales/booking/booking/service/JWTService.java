package com.aviasales.booking.booking.service;


import com.aviasales.booking.booking.projection.UsersProjection;
import com.aviasales.booking.booking.repo.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;


@Slf4j
@Service
@RequiredArgsConstructor
public class JWTService {
    private final SecretKey secretKey;
    private final UserRepository userRepository;

    public String generateToken(String email) {
        UsersProjection user = userRepository.findByEmail(email);

        return Jwts.builder()
                .subject(email)
                .claim("id", user.getId())
                .claim("roles", user.getRoleNames())
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000))
                .signWith(secretKey)
                .compact();
    }


    public String refreshToken(String email) {
        UsersProjection user = userRepository.findByEmail(email);

        return Jwts.builder()
                .subject(email)
                .claim("id", user.getId())
                .claim("roles", user.getRoleNames())
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))
                .signWith(secretKey)
                .compact();
    }

    public boolean isValid(String authorization) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(authorization);

            return true;
        }catch (Exception e) {
            log.error("JWT validation failed", e);
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        try {
            Claims payload = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return payload.getSubject();
        } catch (Exception e) {
            Claims payload = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return payload.getSubject();
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims payload = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = payload.get("type").toString();
            return "refresh".equals(type);
        } catch (Exception e) {
            log.error("JWT validation failed", e);
            return false;
        }
    }

    public UsersProjection getUserObject(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        log.info("User loaded from claims: {}", claims);

        return userRepository.findByEmail(claims.getSubject());
    }
}
