package com.ayush.ecommerce_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Name can't be blank!!")
    private String name;
}
