package com.ayush.ecommerce_backend.controller;

import com.ayush.ecommerce_backend.dto.AdminUserResponse;
import com.ayush.ecommerce_backend.dto.UpdateUserRoleRequest;
import com.ayush.ecommerce_backend.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@AllArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;


    // =========================
    // GET ALL USERS
    // =========================

    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> getAllUsers(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "id")
            String sortBy,

            @RequestParam(defaultValue = "asc")
            String direction,

            @RequestParam(required = false)
            String search
    ) {

        Page<AdminUserResponse> users =
                adminUserService.getAllUsers(
                        page,
                        size,
                        sortBy,
                        direction,
                        search
                );

        return ResponseEntity.ok(users);
    }


    // =========================
    // GET USER
    // =========================

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> getUser(
            @PathVariable Integer id
    ) {

        return ResponseEntity.ok(
                adminUserService.getUser(id)
        );
    }


    // =========================
    // ENABLE / DISABLE USER
    // =========================

    @PutMapping("/{id}/status")
    public ResponseEntity<AdminUserResponse> updateUserStatus(

            @PathVariable Integer id,

            @RequestParam boolean enabled
    ) {

        return ResponseEntity.ok(
                adminUserService.updateUserStatus(
                        id,
                        enabled
                )
        );
    }


    // =========================
    // CHANGE ROLE
    // =========================

    @PutMapping("/{id}/role")
    public ResponseEntity<AdminUserResponse> updateUserRole(

            @PathVariable Integer id,

            @Valid @RequestBody
            UpdateUserRoleRequest request
    ) {

        return ResponseEntity.ok(
                adminUserService.updateUserRole(
                        id,
                        request
                )
        );
    }


    // =========================
    // DELETE USER
    // =========================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Integer id
    ) {

        adminUserService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }
}