package com.ayush.ecommerce_backend.controller;

import com.ayush.ecommerce_backend.dto.ProductRequest;
import com.ayush.ecommerce_backend.dto.ProductResponse;
import com.ayush.ecommerce_backend.service.ProductService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@AllArgsConstructor
public class ProductController {

    private final ProductService productService;


    // =========================
    // CREATE
    // ADMIN ONLY
    // =========================

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request
    ) {

        ProductResponse response =
                productService.createProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =========================
    // GET ALL PRODUCTS
    // =========================

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "id")
            String sortBy,

            @RequestParam(defaultValue = "asc")
            String direction,

            @RequestParam(required = false)
            Integer categoryId,

            @RequestParam(required = false)
            String search
    ) {

        Page<ProductResponse> products =
                productService.getAllProducts(
                        page,
                        size,
                        sortBy,
                        direction,
                        categoryId,
                        search
                );

        return ResponseEntity.ok(products);
    }


    // =========================
    // GET BY ID
    // =========================

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable Integer id
    ) {

        ProductResponse response =
                productService.getProduct(id);

        return ResponseEntity.ok(response);
    }


    // =========================
    // UPDATE
    // ADMIN ONLY
    // =========================

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Integer id,
            @Valid @RequestBody ProductRequest request
    ) {

        ProductResponse response =
                productService.updateProduct(id, request);

        return ResponseEntity.ok(response);
    }


    // =========================
    // DELETE
    // ADMIN ONLY
    // =========================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Integer id
    ) {

        productService.deleteProduct(id);

        return ResponseEntity.noContent().build();
    }
}