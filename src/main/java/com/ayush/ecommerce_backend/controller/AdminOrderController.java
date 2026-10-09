package com.ayush.ecommerce_backend.controller;

import com.ayush.ecommerce_backend.dto.OrderResponse;
import com.ayush.ecommerce_backend.dto.UpdateOrderStatusRequest;
import com.ayush.ecommerce_backend.enums.OrderStatus;
import com.ayush.ecommerce_backend.service.AdminOrderService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@AllArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    // GET ALL ORDERS
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "desc")
            String direction,

            @RequestParam(required = false)
            OrderStatus status,

            @RequestParam(required = false)
            String userEmail
    ) {

        Page<OrderResponse> orders =
                adminOrderService.getAllOrders(
                        page,
                        size,
                        direction,
                        status,
                        userEmail
                );

        return ResponseEntity.ok(orders);
    }


    // GET SINGLE ORDER
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Integer id
    ) {

        return ResponseEntity.ok(
                adminOrderService.getOrder(id)
        );
    }


    // UPDATE STATUS
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(

            @PathVariable Integer id,

            @Valid
            @RequestBody
            UpdateOrderStatusRequest request
    ) {

        return ResponseEntity.ok(
                adminOrderService.updateOrderStatus(
                        id,
                        request
                )
        );
    }
}