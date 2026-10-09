
package com.ayush.ecommerce_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private Jwt encodedJwt;

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "issuer", "ayush-api");
        ReflectionTestUtils.setField(jwtService, "expiry", 900L);

        when(encodedJwt.getTokenValue()).thenReturn("test-jwt-token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(encodedJwt);
    }

    @Test
    void generateToken_shouldReturnEncodedToken() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "user@example.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        String token = jwtService.generateToken(authentication);

        assertEquals("test-jwt-token", token);

        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }

    @Test
    void generateToken_shouldIncludeIssuerSubjectAndExpiry() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "user@example.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        Instant beforeGeneration = Instant.now();

        jwtService.generateToken(authentication);

        ArgumentCaptor<JwtEncoderParameters> captor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        verify(jwtEncoder).encode(captor.capture());

        JwtClaimsSet claims = captor.getValue().getClaims();

        assertEquals("ayush-api", claims.getClaimAsString("iss"));
        assertEquals("user@example.com", claims.getSubject());

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiresAt());

        assertFalse(claims.getIssuedAt().isBefore(beforeGeneration));

        assertEquals(
                900L,
                claims.getExpiresAt().getEpochSecond()
                        - claims.getIssuedAt().getEpochSecond()
        );
    }

    @Test
    void generateToken_shouldIncludeRolesButExcludeNonRoleAuthorities() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "admin@example.com",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("SCOPE_read")
                        )
                );

        jwtService.generateToken(authentication);

        ArgumentCaptor<JwtEncoderParameters> captor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        verify(jwtEncoder).encode(captor.capture());

        JwtClaimsSet claims = captor.getValue().getClaims();

        List<?> authorities = claims.getClaim("authorities");

        assertEquals(
                List.of("ROLE_ADMIN", "ROLE_USER"),
                authorities
        );

        assertFalse(authorities.contains("SCOPE_read"));
    }
}
