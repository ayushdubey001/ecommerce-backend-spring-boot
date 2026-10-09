package com.ayush.ecommerce_backend.controller;

import com.ayush.ecommerce_backend.dto.AddToCartRequest;
import com.ayush.ecommerce_backend.dto.CartResponse;
import com.ayush.ecommerce_backend.dto.UpdateCartItemRequest;
import com.ayush.ecommerce_backend.service.CartService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@AllArgsConstructor
public class CartController {

    private final CartService cartService;


    @PostMapping("/items")
    public ResponseEntity<Void> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication authentication
    ) {

        cartService.addToCart(
                authentication.getName(),
                request
        );

        return ResponseEntity.ok().build();
    }


    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                cartService.getCart(
                        authentication.getName()
                )
        );
    }


    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<Void> updateCartItem(
            @PathVariable Integer cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication
    ) {

        cartService.updateCartItem(
                cartItemId,
                request,
                authentication.getName()
        );

        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> deleteCartItem(
            @PathVariable Integer cartItemId,
            Authentication authentication
    ) {

        cartService.deleteCartItem(
                cartItemId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/items")
    public ResponseEntity<Void> clearCart(
            Authentication authentication
    ) {

        cartService.clearCart(
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }
}