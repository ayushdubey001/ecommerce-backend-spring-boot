package com.ayush.ecommerce_backend.controller;

import com.ayush.ecommerce_backend.dto.CategoryRequest;
import com.ayush.ecommerce_backend.dto.CategoryResponse;
import com.ayush.ecommerce_backend.exception.GlobalExceptionHandler;
import com.ayush.ecommerce_backend.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void createCategory_shouldReturnCreated() throws Exception {
        CategoryResponse response = category(1, "Books");
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Books\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Books"));

        verify(categoryService).createCategory(any(CategoryRequest.class));
    }

    @Test
    void createCategory_withBlankName_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.messages.name").exists());
    }

    @Test
    void getAllCategories_shouldReturnFoundAndList() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of(category(1, "Books"), category(2, "Games")));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isFound())
                .andExpect(jsonPath("$[0].name").value("Books"))
                .andExpect(jsonPath("$[1].name").value("Games"));
    }

    @Test
    void getCategory_shouldReturnCategory() throws Exception {
        when(categoryService.getCategory(1)).thenReturn(category(1, "Books"));

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Books"));
    }

    @Test
    void updateCategory_shouldReturnAccepted() throws Exception {
        when(categoryService.updateCategory(eq(1), any(CategoryRequest.class))).thenReturn(category(1, "Updated Books"));

        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Books\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.name").value("Updated Books"));
    }

    @Test
    void deleteCategory_shouldReturnOk() throws Exception {
        doNothing().when(categoryService).deleteCategory(1);

        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isOk());

        verify(categoryService).deleteCategory(1);
    }

    private CategoryResponse category(Integer id, String name) {
        CategoryResponse response = new CategoryResponse();
        response.setId(id);
        response.setName(name);
        return response;
    }
}
