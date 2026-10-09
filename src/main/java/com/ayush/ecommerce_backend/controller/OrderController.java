package com.ayush.ecommerce_backend.controller;

import com.ayush.ecommerce_backend.dto.OrderResponse;
import com.ayush.ecommerce_backend.enums.OrderStatus;
import com.ayush.ecommerce_backend.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // CHECKOUT
    @PostMapping
    public ResponseEntity<OrderResponse> checkout(
            Authentication authentication
    ) {

        OrderResponse response =
                orderService.checkout(
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // GET MY ORDERS
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(

            Authentication authentication,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "desc")
            String direction
    ) {

        Page<OrderResponse> orders =
                orderService.getAllOrders(
                        authentication.getName(),
                        page,
                        size,
                        direction
                );

        return ResponseEntity.ok(orders);
    }


    // GET MY ORDERS BY STATUS
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<OrderResponse>> getOrdersByStatus(

            @PathVariable OrderStatus status,

            Authentication authentication,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "desc")
            String direction
    ) {

        Page<OrderResponse> orders =
                orderService.getOrdersByStatus(
                        authentication.getName(),
                        status,
                        page,
                        size,
                        direction
                );

        return ResponseEntity.ok(orders);
    }


    // GET SINGLE ORDER
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(

            @PathVariable Integer id,

            Authentication authentication
    ) {

        OrderResponse response =
                orderService.getOrder(
                        id,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }


    // CANCEL ORDER
    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(

            @PathVariable Integer id,

            Authentication authentication
    ) {

        OrderResponse response =
                orderService.cancelOrder(
                        id,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }
}