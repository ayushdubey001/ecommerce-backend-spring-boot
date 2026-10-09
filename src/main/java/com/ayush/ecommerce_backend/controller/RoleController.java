package com.ayush.ecommerce_backend.controller;


import com.ayush.ecommerce_backend.dto.RoleRequest;
import com.ayush.ecommerce_backend.service.RoleService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/role")
@AllArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping("/create")
    public ResponseEntity<Void> createRole(@RequestBody RoleRequest roleRequest){
        roleService.createRole(roleRequest);
        return ResponseEntity.ok().build();
    }
}
