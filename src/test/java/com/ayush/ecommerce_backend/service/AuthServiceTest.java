
package com.ayush.ecommerce_backend.service;

import com.ayush.ecommerce_backend.dto.LoginRequest;
import com.ayush.ecommerce_backend.dto.LoginResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_shouldReturnJwtTokenForValidCredentials() {
        LoginRequest request = new LoginRequest(
                "user@example.com",
                "password123"
        );

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenReturn(authentication);

        when(jwtService.generateToken(authentication))
                .thenReturn("test-jwt-token");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("test-jwt-token", response.getAccessToken());

        verify(authenticationManager).authenticate(
                argThat(authRequest ->
                        "user@example.com".equals(authRequest.getPrincipal())
                                && "password123".equals(authRequest.getCredentials())
                )
        );

        verify(jwtService).generateToken(authentication);
    }

    @Test
    void login_shouldNormalizeEmailBeforeAuthentication() {
        LoginRequest request = new LoginRequest(
                "  USER@EXAMPLE.COM  ",
                "password123"
        );

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenReturn(authentication);

        when(jwtService.generateToken(authentication))
                .thenReturn("test-jwt-token");

        authService.login(request);

        verify(authenticationManager).authenticate(
                argThat(authRequest ->
                        "user@example.com".equals(authRequest.getPrincipal())
                )
        );
    }

    @Test
    void login_shouldNotGenerateTokenWhenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest(
                "user@example.com",
                "wrong-password"
        );

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        verify(jwtService, never()).generateToken(any(Authentication.class));
    }

    @Test
    void login_shouldPropagateAuthenticationFailure() {
        LoginRequest request = new LoginRequest(
                "disabled@example.com",
                "password123"
        );

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenThrow(new IllegalStateException("Authentication failed"));

        assertThrows(
                IllegalStateException.class,
                () -> authService.login(request)
        );

        verifyNoInteractions(jwtService);
    }
}
