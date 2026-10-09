package com.ayush.ecommerce_backend.controller;

import com.ayush.ecommerce_backend.dto.ProductRequest;
import com.ayush.ecommerce_backend.dto.ProductResponse;
import com.ayush.ecommerce_backend.enums.Availability;
import com.ayush.ecommerce_backend.exception.GlobalExceptionHandler;
import com.ayush.ecommerce_backend.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void createProduct_shouldReturnCreated() throws Exception {
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(product(1, "Keyboard"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.price").value(999.00));

        verify(productService).createProduct(any(ProductRequest.class));
    }

    @Test
    void createProduct_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"description\":\"\",\"price\":-1,\"stock\":-2,\"categoryId\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.messages.name").exists())
                .andExpect(jsonPath("$.messages.price").exists())
                .andExpect(jsonPath("$.messages.stock").exists())
                .andExpect(jsonPath("$.messages.categoryId").exists());
    }

    @Test
    void getAllProducts_shouldReturnPage() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(product(1, "Keyboard")), PageRequest.of(0, 10), 1);
        when(productService.getAllProducts(0, 10, "id", "asc", null, null)).thenReturn(page);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Keyboard"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAllProducts_shouldPassSearchAndCategoryFilters() throws Exception {
        when(productService.getAllProducts(2, 5, "name", "desc", 7, "keyboard"))
                .thenReturn(new PageImpl<>(List.of(product(1, "Keyboard")), PageRequest.of(2, 5), 1));

        mockMvc.perform(get("/api/products")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sortBy", "name")
                        .param("direction", "desc")
                        .param("categoryId", "7")
                        .param("search", "keyboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Keyboard"));

        verify(productService).getAllProducts(2, 5, "name", "desc", 7, "keyboard");
    }

    @Test
    void getProduct_shouldReturnProduct() throws Exception {
        when(productService.getProduct(1)).thenReturn(product(1, "Keyboard"));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Keyboard"));
    }

    @Test
    void updateProduct_shouldReturnUpdatedProduct() throws Exception {
        when(productService.updateProduct(eq(1), any(ProductRequest.class))).thenReturn(product(1, "Mechanical Keyboard"));

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mechanical Keyboard\",\"description\":\"RGB keyboard\",\"price\":1999.00,\"stock\":12,\"categoryId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mechanical Keyboard"));
    }

    @Test
    void deleteProduct_shouldReturnNoContent() throws Exception {
        doNothing().when(productService).deleteProduct(1);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1);
    }

    private String validProductJson() {
        return "{\"name\":\"Keyboard\",\"description\":\"Wireless keyboard\",\"price\":999.00,\"stock\":10,\"categoryId\":1}";
    }

    private ProductResponse product(Integer id, String name) {
        return new ProductResponse(
                id,
                name,
                "Wireless keyboard",
                new BigDecimal("999.00"),
                10,
                "Accessories",
                Availability.IN_STOCK,
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );
    }
}
