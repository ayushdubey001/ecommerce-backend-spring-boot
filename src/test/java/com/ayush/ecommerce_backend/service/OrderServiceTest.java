
package com.ayush.ecommerce_backend.service;

import com.ayush.ecommerce_backend.dto.OrderResponse;
import com.ayush.ecommerce_backend.entity.Cart;
import com.ayush.ecommerce_backend.entity.CartItem;
import com.ayush.ecommerce_backend.entity.Order;
import com.ayush.ecommerce_backend.entity.OrderItem;
import com.ayush.ecommerce_backend.entity.Product;
import com.ayush.ecommerce_backend.entity.Users;
import com.ayush.ecommerce_backend.enums.OrderStatus;
import com.ayush.ecommerce_backend.exception.ForbiddenException;
import com.ayush.ecommerce_backend.exception.InsufficientStockException;
import com.ayush.ecommerce_backend.exception.InvalidOrderStatusException;
import com.ayush.ecommerce_backend.exception.ResourceNotFoundException;
import com.ayush.ecommerce_backend.repository.CartItemRepository;
import com.ayush.ecommerce_backend.repository.CartRepository;
import com.ayush.ecommerce_backend.repository.OrderItemRepository;
import com.ayush.ecommerce_backend.repository.OrderRepository;
import com.ayush.ecommerce_backend.repository.ProductRepository;
import com.ayush.ecommerce_backend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private Users user;
    private Users anotherUser;
    private Cart cart;
    private Product product;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        user = new Users();
        user.setId(1);
        user.setName("Test User");
        user.setEmail("user@example.com");

        anotherUser = new Users();
        anotherUser.setId(2);
        anotherUser.setName("Another User");
        anotherUser.setEmail("another@example.com");

        cart = new Cart();
        cart.setId(1);
        cart.setUser(user);

        product = new Product();
        product.setId(1);
        product.setName("Keyboard");
        product.setPrice(new BigDecimal("1500.00"));
        product.setStock(10);

        cartItem = new CartItem();
        cartItem.setId(1);
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
    }

    private void mockUserCartAndItems() {
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUser(user))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCart(cart))
                .thenReturn(List.of(cartItem));
    }

    private Order createOrder(OrderStatus status) {
        Order order = new Order();
        order.setId(10);
        order.setUser(user);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("3000.00"));
        order.setCreatedAt(java.time.LocalDateTime.now());

        OrderItem item = new OrderItem();
        item.setId(20);
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(2);
        item.setPrice(new BigDecimal("1500.00"));

        order.setOrderItems(new ArrayList<>(List.of(item)));
        return order;
    }

    // CHECKOUT

    @Test
    void checkout_shouldCreateOrderAndReduceStock() {
        mockUserCartAndItems();

        when(productRepository.findByIdForUpdate(1))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(10);
                    return order;
                });

        OrderResponse response =
                orderService.checkout("user@example.com");

        assertEquals(10, response.getOrderId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("3000.00"),
                response.getTotalAmount());

        assertEquals(8, product.getStock());

        assertEquals(1, response.getItems().size());
        assertEquals(2, response.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("1500.00"),
                response.getItems().get(0).getPrice());
        assertEquals(new BigDecimal("3000.00"),
                response.getItems().get(0).getSubtotal());

        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(anyList());
        verify(cartItemRepository).deleteAll(List.of(cartItem));
    }

    @Test
    void checkout_shouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.checkout("missing@example.com")
        );

        verifyNoInteractions(cartRepository, cartItemRepository);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkout_shouldThrowWhenCartDoesNotExist() {
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUser(user))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.checkout("user@example.com")
        );

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkout_shouldRejectEmptyCart() {
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUser(user))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCart(cart))
                .thenReturn(List.of());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.checkout("user@example.com")
        );

        verify(orderRepository, never()).save(any(Order.class));
        verify(productRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void checkout_shouldThrowWhenProductDoesNotExist() {
        mockUserCartAndItems();

        when(productRepository.findByIdForUpdate(1))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.checkout("user@example.com")
        );

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkout_shouldRejectInsufficientStock() {
        mockUserCartAndItems();
        product.setStock(1);

        when(productRepository.findByIdForUpdate(1))
                .thenReturn(Optional.of(product));

        assertThrows(
                InsufficientStockException.class,
                () -> orderService.checkout("user@example.com")
        );

        assertEquals(1, product.getStock());
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartItemRepository, never()).deleteAll(anyList());
    }

    @Test
    void checkout_shouldUseCurrentProductPrice() {
        mockUserCartAndItems();
        product.setPrice(new BigDecimal("2000.00"));

        when(productRepository.findByIdForUpdate(1))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(11);
                    return order;
                });

        OrderResponse response =
                orderService.checkout("user@example.com");

        assertEquals(new BigDecimal("4000.00"),
                response.getTotalAmount());
        assertEquals(new BigDecimal("2000.00"),
                response.getItems().get(0).getPrice());
    }

    // GET ORDER

    @Test
    void getOrder_shouldReturnOwnedOrder() {
        Order order = createOrder(OrderStatus.PENDING);

        when(orderRepository.findById(10))
                .thenReturn(Optional.of(order));

        OrderResponse response =
                orderService.getOrder(10, "user@example.com");

        assertEquals(10, response.getOrderId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("3000.00"),
                response.getTotalAmount());
    }

    @Test
    void getOrder_shouldRejectAnotherUsersOrder() {
        Order order = createOrder(OrderStatus.PENDING);
        order.setUser(anotherUser);

        when(orderRepository.findById(10))
                .thenReturn(Optional.of(order));

        assertThrows(
                ForbiddenException.class,
                () -> orderService.getOrder(10, "user@example.com")
        );
    }

    @Test
    void getOrder_shouldThrowWhenOrderDoesNotExist() {
        when(orderRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getOrder(99, "user@example.com")
        );
    }

    // CANCEL ORDER

    @Test
    void cancelOrder_shouldRestoreStockAndCancelOrder() {
        Order order = createOrder(OrderStatus.PENDING);

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(orderRepository.findById(10))
                .thenReturn(Optional.of(order));

        when(productRepository.findByIdForUpdate(1))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(order)).thenReturn(order);

        OrderResponse response =
                orderService.cancelOrder(10, "user@example.com");

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertEquals(12, product.getStock());

        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrder_shouldRejectAnotherUsersOrder() {
        Order order = createOrder(OrderStatus.PENDING);
        order.setUser(anotherUser);

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(orderRepository.findById(10))
                .thenReturn(Optional.of(order));

        assertThrows(
                ForbiddenException.class,
                () -> orderService.cancelOrder(10, "user@example.com")
        );

        verify(productRepository, never()).findByIdForUpdate(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelOrder_shouldRejectNonPendingOrder() {
        Order order = createOrder(OrderStatus.SHIPPED);

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(orderRepository.findById(10))
                .thenReturn(Optional.of(order));

        assertThrows(
                InvalidOrderStatusException.class,
                () -> orderService.cancelOrder(10, "user@example.com")
        );

        verify(productRepository, never()).findByIdForUpdate(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelOrder_shouldThrowWhenOrderDoesNotExist() {
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(orderRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.cancelOrder(99, "user@example.com")
        );
    }

    // ORDER HISTORY AND PAGINATION

    @Test
    void getAllOrders_shouldReturnUsersOrders() {
        Order order = createOrder(OrderStatus.PENDING);
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(orderRepository.findByUser(
                eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        Page<OrderResponse> response =
                orderService.getAllOrders(
                        "user@example.com", 0, 10, "desc");

        assertEquals(1, response.getTotalElements());
        assertEquals(10, response.getContent().get(0).getOrderId());
    }

    @Test
    void getOrdersByStatus_shouldReturnMatchingOrders() {
        Order order = createOrder(OrderStatus.PENDING);
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(orderRepository.findByUserAndStatus(
                eq(user), eq(OrderStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        Page<OrderResponse> response =
                orderService.getOrdersByStatus(
                        "user@example.com",
                        OrderStatus.PENDING,
                        0, 10, "desc");

        assertEquals(1, response.getTotalElements());
        assertEquals(OrderStatus.PENDING,
                response.getContent().get(0).getStatus());
    }

    @Test
    void getAllOrders_shouldRejectNegativePage() {
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.getAllOrders(
                        "user@example.com", -1, 10, "desc")
        );
    }

    @Test
    void getAllOrders_shouldRejectInvalidPageSize() {
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.getAllOrders(
                        "user@example.com", 0, 101, "desc")
        );
    }

    @Test
    void getAllOrders_shouldRejectInvalidSortDirection() {
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.getAllOrders(
                        "user@example.com", 0, 10, "sideways")
        );
    }
}
