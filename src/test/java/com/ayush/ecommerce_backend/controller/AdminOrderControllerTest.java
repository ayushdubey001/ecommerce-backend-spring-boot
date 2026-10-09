package com.ayush.ecommerce_backend.controller;

import com.ayush.ecommerce_backend.dto.OrderResponse;
import com.ayush.ecommerce_backend.dto.UpdateOrderStatusRequest;
import com.ayush.ecommerce_backend.enums.OrderStatus;
import com.ayush.ecommerce_backend.exception.GlobalExceptionHandler;
import com.ayush.ecommerce_backend.service.AdminOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOrderController.class)
@Import(GlobalExceptionHandler.class)
class AdminOrderControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminOrderService adminOrderService;

    @Test
    void getAllOrders_shouldReturnPage() throws Exception {
        when(adminOrderService.getAllOrders(0, 10, "desc", null, null))
                .thenReturn(new PageImpl<>(List.of(order(1, OrderStatus.PENDING))));

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(1));

        verify(adminOrderService).getAllOrders(0, 10, "desc", null, null);
    }

    @Test
    void getAllOrders_withStatusAndEmail_shouldPassFilters() throws Exception {
        when(adminOrderService.getAllOrders(1, 5, "asc", OrderStatus.PENDING, "user@example.com"))
                .thenReturn(new PageImpl<>(List.of(order(2, OrderStatus.PENDING))));

        mockMvc.perform(get("/api/admin/orders")
                        .param("page", "1").param("size", "5")
                        .param("direction", "asc").param("status", "PENDING")
                        .param("userEmail", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(2));
    }

    @Test
    void getOrder_shouldReturnOrder() throws Exception {
        when(adminOrderService.getOrder(7)).thenReturn(order(7, OrderStatus.PENDING));

        mockMvc.perform(get("/api/admin/orders/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(7))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void updateOrderStatus_shouldReturnUpdatedOrder() throws Exception {
        when(adminOrderService.updateOrderStatus(eq(7), any(UpdateOrderStatusRequest.class)))
                .thenReturn(order(7, OrderStatus.CONFIRMED));

        mockMvc.perform(put("/api/admin/orders/7/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(adminOrderService).updateOrderStatus(eq(7), any(UpdateOrderStatusRequest.class));
    }

    @Test
    void updateOrderStatus_withMissingStatus_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/admin/orders/7/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private OrderResponse order(Integer id, OrderStatus status) {
        return new OrderResponse(id, new BigDecimal("25.00"), status, LocalDateTime.now(), List.of());
    }
}
