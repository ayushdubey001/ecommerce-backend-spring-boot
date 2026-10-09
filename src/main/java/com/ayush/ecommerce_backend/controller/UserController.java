package com.ayush.ecommerce_backend.controller;

import com.ayush.ecommerce_backend.dto.ChangePasswordRequest;
import com.ayush.ecommerce_backend.dto.RegisterRequest;
import com.ayush.ecommerce_backend.dto.UpdateUserRequest;
import com.ayush.ecommerce_backend.dto.UserResponse;
import com.ayush.ecommerce_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody RegisterRequest request
    ) {

        UserResponse response =
                userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // CURRENT USER
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            Authentication authentication
    ) {

        UserResponse response =
                userService.getCurrentUser(
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }


    // UPDATE CURRENT USER
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication
    ) {

        UserResponse response =
                userService.updateCurrentUser(
                        authentication.getName(),
                        request
                );

        return ResponseEntity.ok(response);
    }


    // CHANGE PASSWORD
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {

        userService.changePassword(
                authentication.getName(),
                request
        );

        return ResponseEntity.noContent().build();
    }
}