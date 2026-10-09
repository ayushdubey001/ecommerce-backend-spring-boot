package com.ayush.ecommerce_backend.dto;

import com.ayush.ecommerce_backend.enums.Availability;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {

    private Integer id;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private String category;

    private Availability availability;

    private LocalDateTime createdAt;
}