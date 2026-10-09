package com.ayush.ecommerce_backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "Product description is required")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Price cannot be negative"
    )
    private BigDecimal price;

    @NotNull(message = "Stock is required")
    @Min(
            value = 0,
            message = "Stock cannot be negative"
    )
    private Integer stock;

    @NotNull(message = "Category ID is required")
    private Integer categoryId;
}