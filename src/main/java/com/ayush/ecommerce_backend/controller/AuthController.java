package com.ayush.ecommerce_backend.controller;

import com.ayush.ecommerce_backend.dto.LoginRequest;
import com.ayush.ecommerce_backend.dto.LoginResponse;
import com.ayush.ecommerce_backend.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@Tag(
        name = "Authentication",
        description = "APIs for user authentication and JWT token generation"
)
@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login( @Valid @RequestBody LoginRequest loginRequest){
        LoginResponse loginResponse = authService.login(loginRequest);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(loginResponse);
    }

}
