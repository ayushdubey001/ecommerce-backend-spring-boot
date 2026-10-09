package com.ayush.ecommerce_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserResponse {

    private Integer id;

    private String name;

    private String email;

    private boolean enabled;

    private Set<String> roles;
}