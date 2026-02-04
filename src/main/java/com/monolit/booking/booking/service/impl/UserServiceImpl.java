package com.monolit.booking.booking.service.impl;


import com.monolit.booking.booking.dto.JwtDTO;
import com.monolit.booking.booking.dto.LoginRequest;
import com.monolit.booking.booking.entity.Roles;
import com.monolit.booking.booking.entity.Users;
import com.monolit.booking.booking.repo.RoleRepository;
import com.monolit.booking.booking.repo.UserRepository;
import com.monolit.booking.booking.service.JWTService;

import com.monolit.booking.booking.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.monolit.booking.booking.enums.UserRole.ROLE_USER;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    public void registerUser(LoginRequest dto) {
        if (userRepository.findByEmail(dto.getEmail()) != null) throw new IllegalArgumentException("Email already in use");

        List<Roles> roles = List.of(new Roles(ROLE_USER));
        roleRepository.saveAll(roles);

        Users user = Users.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(roles)
                .build();

        userRepository.save(user);
    }

    public JwtDTO loginUser(LoginRequest loginRequest) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken
                (loginRequest.getEmail(), loginRequest.getPassword());
        authenticationManager.authenticate(auth);

        String accessToken = jwtService.generateToken(loginRequest.getEmail());
        String refreshToken = jwtService.refreshToken(loginRequest.getEmail());
        return new JwtDTO(accessToken, refreshToken);
    }

    public JwtDTO refreshToken(String refreshTokenAuth) {
        if (refreshTokenAuth == null) throw new IllegalArgumentException("Refresh token is null");
        if (!jwtService.isRefreshToken(refreshTokenAuth)) throw new IllegalArgumentException("This is not a refresh token");

        String email = jwtService.getEmailFromToken(refreshTokenAuth);
        String newAccessToken = jwtService.generateToken(email);
        String newRefreshToken = jwtService.refreshToken(email);

        return new JwtDTO(newAccessToken, newRefreshToken);
    }

}
