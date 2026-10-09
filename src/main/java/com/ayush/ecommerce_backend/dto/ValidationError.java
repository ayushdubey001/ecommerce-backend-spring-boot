package com.ayush.ecommerce_backend.dto;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ValidationError {
    private String field;
    private String message;

}
