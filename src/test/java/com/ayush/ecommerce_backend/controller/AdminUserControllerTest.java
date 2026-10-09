package com.ayush.ecommerce_backend.controller;

import com.ayush.ecommerce_backend.dto.AdminUserResponse;
import com.ayush.ecommerce_backend.dto.UpdateUserRoleRequest;
import com.ayush.ecommerce_backend.exception.GlobalExceptionHandler;
import com.ayush.ecommerce_backend.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@Import(GlobalExceptionHandler.class)
class AdminUserControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminUserService adminUserService;

    @Test
    void getAllUsers_shouldReturnPage() throws Exception {
        when(adminUserService.getAllUsers(0, 10, "id", "asc", null))
                .thenReturn(new PageImpl<>(List.of(user(1, "user@example.com", true))));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].email").value("user@example.com"));
    }

    @Test
    void getAllUsers_withSearchAndSorting_shouldPassParameters() throws Exception {
        when(adminUserService.getAllUsers(0, 20, "email", "desc", "ayush"))
                .thenReturn(new PageImpl<>(List.of(user(2, "ayush@example.com", true))));

        mockMvc.perform(get("/api/admin/users").param("size", "20")
                        .param("sortBy", "email").param("direction", "desc").param("search", "ayush"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("ayush@example.com"));

        verify(adminUserService).getAllUsers(0, 20, "email", "desc", "ayush");
    }

    @Test
    void getUser_shouldReturnUser() throws Exception {
        when(adminUserService.getUser(3)).thenReturn(user(3, "user@example.com", true));

        mockMvc.perform(get("/api/admin/users/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void updateUserStatus_shouldReturnUpdatedUser() throws Exception {
        when(adminUserService.updateUserStatus(3, false)).thenReturn(user(3, "user@example.com", false));

        mockMvc.perform(put("/api/admin/users/3/status").param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void updateUserRole_shouldReturnUpdatedUser() throws Exception {
        when(adminUserService.updateUserRole(eq(3), any(UpdateUserRoleRequest.class)))
                .thenReturn(new AdminUserResponse(3, "Ayush", "user@example.com", true, Set.of("ROLE_ADMIN")));

        mockMvc.perform(put("/api/admin/users/3/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
    }

    @Test
    void updateUserRole_withBlankRole_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/admin/users/3/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_shouldReturnNoContent() throws Exception {
        doNothing().when(adminUserService).deleteUser(3);

        mockMvc.perform(delete("/api/admin/users/3"))
                .andExpect(status().isNoContent());

        verify(adminUserService).deleteUser(3);
    }

    private AdminUserResponse user(Integer id, String email, boolean enabled) {
        return new AdminUserResponse(id, "Test User", email, enabled, Set.of("ROLE_USER"));
    }
}
