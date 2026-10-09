package com.ayush.ecommerce_backend.controller;

import com.ayush.ecommerce_backend.dto.LoginRequest;
import com.ayush.ecommerce_backend.dto.LoginResponse;
import com.ayush.ecommerce_backend.exception.GlobalExceptionHandler;
import com.ayush.ecommerce_backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AuthService authService;

    @Test
    void login_withValidRequest_shouldReturnToken() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(new LoginResponse("test-jwt-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-jwt-token"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void login_withInvalidEmail_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withBlankPassword_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\" \"}"))
                .andExpect(status().isBadRequest());
    }
}
