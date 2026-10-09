package com.ayush.ecommerce_backend.service;

import com.ayush.ecommerce_backend.dto.OrderItemResponse;
import com.ayush.ecommerce_backend.dto.OrderResponse;
import com.ayush.ecommerce_backend.dto.UpdateOrderStatusRequest;
import com.ayush.ecommerce_backend.entity.*;
import com.ayush.ecommerce_backend.enums.OrderStatus;
import com.ayush.ecommerce_backend.exception.ForbiddenException;
import com.ayush.ecommerce_backend.exception.InsufficientStockException;
import com.ayush.ecommerce_backend.exception.InvalidOrderStatusException;
import com.ayush.ecommerce_backend.exception.ResourceNotFoundException;
import com.ayush.ecommerce_backend.repository.*;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;


    // CHECKOUT
    @Transactional
    public OrderResponse checkout(String username) {

        Users user = userRepository.findByEmail(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with username " + username
                        )
                );

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cart not found with username " + username
                        )
                );

        List<CartItem> cartItems =
                cartItemRepository.findByCart(cart);

        if (cartItems.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Cart is empty"
            );
        }

        Order order = new Order();

        order.setUser(user);
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;

        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {

            Product product = productRepository
                    .findByIdForUpdate(
                            cartItem.getProduct().getId()
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Product not found with id "
                                            + cartItem.getProduct().getId()
                            )
                    );

            Integer requestedQuantity =
                    cartItem.getQuantity();

            Integer availableStock =
                    product.getStock();

            if (availableStock == null
                    || requestedQuantity > availableStock) {

                throw new InsufficientStockException(
                        "Insufficient stock for product "
                                + product.getName()
                );
            }

            BigDecimal price =
                    product.getPrice();

            BigDecimal subtotal =
                    price.multiply(
                            BigDecimal.valueOf(
                                    requestedQuantity
                            )
                    );

            total = total.add(subtotal);

            // Reduce stock
            product.setStock(
                    availableStock - requestedQuantity
            );

            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(requestedQuantity);

            // Snapshot price
            orderItem.setPrice(price);

            orderItems.add(orderItem);
        }

        order.setTotalAmount(total);
        order.setOrderItems(orderItems);

        Order savedOrder =
                orderRepository.save(order);

        orderItemRepository.saveAll(orderItems);

        cartItemRepository.deleteAll(cartItems);

        return entityToResponse(savedOrder);
    }


    // USER ORDERS
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(
            String username,
            int page,
            int size,
            String direction
    ) {

        Users user = userRepository.findByEmail(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with username "
                                        + username
                        )
                );

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

        Page<Order> orders =
                orderRepository.findByUser(
                        user,
                        pageable
                );

        return orders.map(this::entityToResponse);
    }


    // USER ORDERS BY STATUS
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByStatus(
            String username,
            OrderStatus status,
            int page,
            int size,
            String direction
    ) {

        Users user = userRepository.findByEmail(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with username "
                                        + username
                        )
                );

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

        Page<Order> orders =
                orderRepository.findByUserAndStatus(
                        user,
                        status,
                        pageable
                );

        return orders.map(this::entityToResponse);
    }


    // GET SINGLE ORDER
    @Transactional(readOnly = true)
    public OrderResponse getOrder(
            Integer id,
            String username
    ) {

        Order order =
                orderRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Order not found with id "
                                                + id
                                )
                        );

        if (!order.getUser()
                .getEmail()
                .equalsIgnoreCase(username)) {

            throw new com.ayush.ecommerce_backend.exception.ForbiddenException(
                    "You are not authorized to access this order"
            );
        }

        return entityToResponse(order);
    }


    // USER CANCEL ORDER
    @Transactional
    public OrderResponse cancelOrder(
            Integer orderId,
            String username
    ) {

        Users user = userRepository.findByEmail(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with username "
                                        + username
                        )
                );

        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with id " + orderId
                        )
                );

        if (!order.getUser()
                .getId()
                .equals(user.getId())) {

            throw new ForbiddenException(
                    "You are not allowed to cancel this order"
            );
        }

        if (order.getStatus() != OrderStatus.PENDING) {

            throw new InvalidOrderStatusException(
                    "Only PENDING orders can be cancelled"
            );
        }

        for (OrderItem item : order.getOrderItems()) {

            Product product = productRepository
                    .findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Product not found with id "
                                            + item.getProduct().getId()
                            )
                    );

            product.setStock(
                    product.getStock() + item.getQuantity()
            );
        }

        order.setStatus(OrderStatus.CANCELLED);

        return entityToResponse(orderRepository.save(order));
    }


    // ENTITY -> RESPONSE
    private OrderResponse entityToResponse(
            Order order
    ) {

        List<OrderItemResponse> itemResponses =
                new ArrayList<>();

        for (OrderItem item : order.getOrderItems()) {

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


    // VALIDATION
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