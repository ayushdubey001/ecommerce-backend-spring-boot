package com.ayush.ecommerce_backend.service;


import com.ayush.ecommerce_backend.dto.RoleRequest;
import com.ayush.ecommerce_backend.entity.Role;
import com.ayush.ecommerce_backend.repository.RoleRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public void createRole(RoleRequest roleRequest) {
        Role role = new Role();
        role.setName(roleRequest.getName());
        roleRepository.save(role);
    }
}
