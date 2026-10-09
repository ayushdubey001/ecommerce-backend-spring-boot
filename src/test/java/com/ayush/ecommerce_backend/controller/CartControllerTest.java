package com.ayush.ecommerce_backend.controller;

import com.ayush.ecommerce_backend.dto.AddToCartRequest;
import com.ayush.ecommerce_backend.dto.CartItemResponse;
import com.ayush.ecommerce_backend.dto.CartResponse;
import com.ayush.ecommerce_backend.dto.UpdateCartItemRequest;
import com.ayush.ecommerce_backend.exception.GlobalExceptionHandler;
import com.ayush.ecommerce_backend.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@Import(GlobalExceptionHandler.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @Test
    void addToCart_shouldReturnOk() throws Exception {
        doNothing().when(cartService).addToCart(eq("user@example.com"), any(AddToCartRequest.class));

        mockMvc.perform(post("/api/cart/items")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":5,\"quantity\":2}"))
                .andExpect(status().isOk());

        verify(cartService).addToCart(eq("user@example.com"), any(AddToCartRequest.class));
    }

    @Test
    void addToCart_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":null,\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.messages.productId").exists())
                .andExpect(jsonPath("$.messages.quantity").exists());
    }

    @Test
    void getCart_shouldReturnCartForAuthenticatedUser() throws Exception {
        CartItemResponse item = new CartItemResponse(11, 5, "Keyboard", new BigDecimal("25.00"), 2, new BigDecimal("50.00"));
        when(cartService.getCart("user@example.com"))
                .thenReturn(new CartResponse(3, List.of(item), new BigDecimal("50.00")));

        mockMvc.perform(get("/api/cart").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(3))
                .andExpect(jsonPath("$.items[0].cartItemId").value(11))
                .andExpect(jsonPath("$.items[0].productName").value("Keyboard"))
                .andExpect(jsonPath("$.totalAmount").value(50.00));

        verify(cartService).getCart("user@example.com");
    }

    @Test
    void updateCartItem_shouldReturnOk() throws Exception {
        doNothing().when(cartService).updateCartItem(eq(11), any(UpdateCartItemRequest.class), eq("user@example.com"));

        mockMvc.perform(put("/api/cart/items/11")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":4}"))
                .andExpect(status().isOk());

        verify(cartService).updateCartItem(eq(11), any(UpdateCartItemRequest.class), eq("user@example.com"));
    }

    @Test
    void updateCartItem_withZeroQuantity_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/cart/items/11")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages.quantity").exists());
    }

    @Test
    void deleteCartItem_shouldReturnNoContent() throws Exception {
        doNothing().when(cartService).deleteCartItem(11, "user@example.com");

        mockMvc.perform(delete("/api/cart/items/11").principal(principal()))
                .andExpect(status().isNoContent());

        verify(cartService).deleteCartItem(11, "user@example.com");
    }

    @Test
    void clearCart_shouldReturnNoContent() throws Exception {
        doNothing().when(cartService).clearCart("user@example.com");

        mockMvc.perform(delete("/api/cart/items").principal(principal()))
                .andExpect(status().isNoContent());

        verify(cartService).clearCart("user@example.com");
    }

    private Principal principal() {
        return new UsernamePasswordAuthenticationToken("user@example.com", "", List.of());
    }
}
