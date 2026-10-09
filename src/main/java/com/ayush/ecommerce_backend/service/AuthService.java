package com.ayush.ecommerce_backend.service;


import com.ayush.ecommerce_backend.dto.LoginRequest;
import com.ayush.ecommerce_backend.dto.LoginResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {

    private AuthenticationManager authenticationManager;
    private JwtService jwtService;

    public LoginResponse login(
            LoginRequest loginRequest
    ) {

        String email = loginRequest.getEmail()
                .trim()
                .toLowerCase();

        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken
                        .unauthenticated(
                                email,
                                loginRequest.getPassword()
                        );

        Authentication authentication =
                authenticationManager.authenticate(
                        authenticationRequest
                );

        String token =
                jwtService.generateToken(
                        authentication
                );

        return new LoginResponse(token);
    }

}
