package com.ayush.ecommerce_backend.controller;

import com.ayush.ecommerce_backend.dto.OrderResponse;
import com.ayush.ecommerce_backend.enums.OrderStatus;
import com.ayush.ecommerce_backend.exception.GlobalExceptionHandler;
import com.ayush.ecommerce_backend.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void checkout_shouldReturnCreatedOrder() throws Exception {
        when(orderService.checkout("user@example.com")).thenReturn(order(21, OrderStatus.PENDING));

        mockMvc.perform(post("/api/orders").principal(authentication()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(21))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(50.00));

        verify(orderService).checkout("user@example.com");
    }

    @Test
    void getAllOrders_shouldReturnPageAndUseDefaults() throws Exception {
        when(orderService.getAllOrders("user@example.com", 0, 10, "desc"))
                .thenReturn(new PageImpl<>(List.of(order(21, OrderStatus.PENDING)), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/orders").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(21))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(orderService).getAllOrders("user@example.com", 0, 10, "desc");
    }

    @Test
    void getAllOrders_shouldPassPaginationParameters() throws Exception {
        when(orderService.getAllOrders("user@example.com", 2, 5, "asc"))
                .thenReturn(new PageImpl<>(List.of(order(22, OrderStatus.CONFIRMED)), PageRequest.of(2, 5), 1));

        mockMvc.perform(get("/api/orders")
                        .principal(authentication())
                        .param("page", "2")
                        .param("size", "5")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(22));

        verify(orderService).getAllOrders("user@example.com", 2, 5, "asc");
    }

    @Test
    void getOrdersByStatus_shouldReturnMatchingOrders() throws Exception {
        when(orderService.getOrdersByStatus("user@example.com", OrderStatus.SHIPPED, 0, 10, "desc"))
                .thenReturn(new PageImpl<>(List.of(order(23, OrderStatus.SHIPPED)), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/orders/status/SHIPPED").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("SHIPPED"));

        verify(orderService).getOrdersByStatus("user@example.com", OrderStatus.SHIPPED, 0, 10, "desc");
    }

    @Test
    void getOrdersByStatus_withInvalidStatus_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/orders/status/NOT_A_STATUS").principal(authentication()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_shouldReturnSingleOrder() throws Exception {
        when(orderService.getOrder(21, "user@example.com")).thenReturn(order(21, OrderStatus.PAID));

        mockMvc.perform(get("/api/orders/21").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(21))
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(orderService).getOrder(21, "user@example.com");
    }

    @Test
    void cancelOrder_shouldReturnCancelledOrder() throws Exception {
        when(orderService.cancelOrder(21, "user@example.com")).thenReturn(order(21, OrderStatus.CANCELLED));

        mockMvc.perform(put("/api/orders/21/cancel").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(21))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(orderService).cancelOrder(21, "user@example.com");
    }

    private Authentication authentication() {
        return new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private OrderResponse order(Integer id, OrderStatus status) {
        return new OrderResponse(id, new BigDecimal("50.00"), status,
                LocalDateTime.of(2026, 10, 10, 10, 0), List.of());
    }
}
