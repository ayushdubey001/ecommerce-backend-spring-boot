package com.ayush.ecommerce_backend.config;

import com.ayush.ecommerce_backend.service.CustomUserDetailsService;
import com.ayush.ecommerce_backend.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void anonymousUser_cannotReadProtectedProductsEndpoint() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    @Test
    void authenticatedUser_canReadProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                        .header("Authorization", bearerToken("ROLE_USER")))
                .andExpect(status().isOk());
    }

    @Test
    void regularUser_cannotCreateProduct() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", bearerToken("ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test product",
                                  "description": "Test description",
                                  "price": 10.00,
                                  "stock": 5,
                                  "categoryId": 1
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    @Test
    void admin_canDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/products/1")
                        .header("Authorization", bearerToken("ROLE_ADMIN")))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1);
    }

    @Test
    void invalidBearerToken_isRejected() throws Exception {
        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    private String bearerToken(String authority) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ayush-api")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .subject("security-test@example.com")
                .claim("authorities", List.of(authority))
                .build();

        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
