package com.ayush.ecommerce_backend.service;

import com.ayush.ecommerce_backend.dto.OrderItemResponse;
import com.ayush.ecommerce_backend.dto.OrderResponse;
import com.ayush.ecommerce_backend.dto.UpdateOrderStatusRequest;
import com.ayush.ecommerce_backend.entity.Order;
import com.ayush.ecommerce_backend.entity.OrderItem;
import com.ayush.ecommerce_backend.entity.Product;
import com.ayush.ecommerce_backend.enums.OrderStatus;
import com.ayush.ecommerce_backend.exception.InvalidOrderStatusException;
import com.ayush.ecommerce_backend.exception.ResourceNotFoundException;
import com.ayush.ecommerce_backend.repository.OrderRepository;
import com.ayush.ecommerce_backend.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;


    // =========================
    // GET ALL ORDERS
    // =========================

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(
            int page,
            int size,
            String direction,
            OrderStatus status,
            String userEmail
    ) {

        validatePagination(page, size);

        Sort.Direction sortDirection =
                parseDirection(direction);

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                sortDirection,
                                "createdAt"
                        )
                );

        Page<Order> orders;

        boolean hasEmail =
                userEmail != null
                        && !userEmail.trim().isEmpty();

        if (hasEmail && status != null) {

            orders =
                    orderRepository
                            .findByUserEmailContainingIgnoreCaseAndStatus(
                                    userEmail.trim(),
                                    status,
                                    pageable
                            );

        } else if (hasEmail) {

            orders =
                    orderRepository
                            .findByUserEmailContainingIgnoreCase(
                                    userEmail.trim(),
                                    pageable
                            );

        } else if (status != null) {

            orders =
                    orderRepository.findByStatus(
                            status,
                            pageable
                    );

        } else {

            orders =
                    orderRepository.findAll(pageable);
        }

        return orders.map(this::entityToResponse);
    }


    // =========================
    // GET ORDER
    // =========================

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Integer id) {

        Order order =
                orderRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Order not found with id "
                                                + id
                                )
                        );

        return entityToResponse(order);
    }


    // =========================
    // UPDATE ORDER STATUS
    // =========================

    @Transactional
    public OrderResponse updateOrderStatus(
            Integer id,
            UpdateOrderStatusRequest request
    ) {

        /*
         * Lock the order row.
         *
         * This prevents two concurrent admin requests
         * from changing the same order based on the
         * same old status.
         */
        Order order =
                orderRepository.findByIdForUpdate(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Order not found with id "
                                                + id
                                )
                        );

        OrderStatus currentStatus =
                order.getStatus();

        OrderStatus newStatus =
                request.getStatus();

        if (!isValidTransition(
                currentStatus,
                newStatus
        )) {

            throw new InvalidOrderStatusException(
                    "Cannot change order status from "
                            + currentStatus
                            + " to "
                            + newStatus
            );
        }


        /*
         * If the admin cancels the order,
         * restore the stock.
         *
         * Product rows are also locked before
         * modifying their stock.
         */
        if (newStatus == OrderStatus.CANCELLED) {

            for (OrderItem item :
                    order.getOrderItems()) {

                Integer productId =
                        item.getProduct().getId();

                Product product =
                        productRepository
                                .findByIdForUpdate(productId)
                                .orElseThrow(() ->
                                        new ResourceNotFoundException(
                                                "Product not found with id "
                                                        + productId
                                        )
                                );

                product.setStock(
                        product.getStock()
                                + item.getQuantity()
                );
            }
        }


        order.setStatus(newStatus);

        return entityToResponse(order);
    }


    // =========================
    // STATUS TRANSITIONS
    // =========================

    private boolean isValidTransition(
            OrderStatus currentStatus,
            OrderStatus newStatus
    ) {

        return switch (currentStatus) {

            case PENDING ->
                    newStatus == OrderStatus.CONFIRMED
                            || newStatus == OrderStatus.CANCELLED;

            case PAID ->
                    newStatus == OrderStatus.CONFIRMED
                            || newStatus == OrderStatus.CANCELLED;

            case CONFIRMED ->
                    newStatus == OrderStatus.SHIPPED;

            case SHIPPED ->
                    newStatus == OrderStatus.DELIVERED;

            case DELIVERED,
                 CANCELLED ->
                    false;
        };
    }


    // =========================
    // ENTITY -> RESPONSE
    // =========================

    private OrderResponse entityToResponse(
            Order order
    ) {

        List<OrderItemResponse> itemResponses =
                new ArrayList<>();

        for (OrderItem item :
                order.getOrderItems()) {

            OrderItemResponse response =
                    new OrderItemResponse();

            response.setProductId(
                    item.getProduct().getId()
            );

            response.setProductName(
                    item.getProduct().getName()
            );

            response.setQuantity(
                    item.getQuantity()
            );

            response.setPrice(
                    item.getPrice()
            );

            response.setSubtotal(
                    item.getPrice()
                            .multiply(
                                    BigDecimal.valueOf(
                                            item.getQuantity()
                                    )
                            )
            );

            itemResponses.add(response);
        }

        OrderResponse response =
                new OrderResponse();

        response.setOrderId(order.getId());

        response.setTotalAmount(
                order.getTotalAmount()
        );

        response.setStatus(
                order.getStatus()
        );

        response.setCreatedAt(
                order.getCreatedAt()
        );

        response.setItems(itemResponses);

        return response;
    }


    // =========================
    // PAGINATION VALIDATION
    // =========================

    private void validatePagination(
            int page,
            int size
    ) {

        if (page < 0) {

            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        if (size <= 0 || size > 100) {

            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }
    }


    // =========================
    // SORT DIRECTION
    // =========================

    private Sort.Direction parseDirection(
            String direction
    ) {

        try {

            return Sort.Direction.fromString(
                    direction
            );

        } catch (IllegalArgumentException exception) {

            throw new IllegalArgumentException(
                    "Invalid sort direction. Use 'asc' or 'desc'"
            );
        }
    }
}