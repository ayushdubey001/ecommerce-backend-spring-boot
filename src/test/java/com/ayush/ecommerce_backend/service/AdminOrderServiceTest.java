package com.ayush.ecommerce_backend.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private AdminOrderService adminOrderService;

    private Order order;
    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(10);
        product.setName("Keyboard");
        product.setPrice(new BigDecimal("25.00"));
        product.setStock(5);

        OrderItem item = new OrderItem();
        item.setId(100);
        item.setProduct(product);
        item.setQuantity(2);
        item.setPrice(new BigDecimal("25.00"));

        order = new Order();
        order.setId(1);
        order.setTotalAmount(new BigDecimal("50.00"));
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setOrderItems(new ArrayList<>(List.of(item)));
    }

    @Test
    void getAllOrders_withoutFilters_returnsMappedPage() {
        when(orderRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        Page<OrderResponse> result = adminOrderService.getAllOrders(0, 10, "asc", null, null);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().get(0).getOrderId());
        assertEquals("50.00", result.getContent().get(0).getTotalAmount().toString());
        assertEquals("Keyboard", result.getContent().get(0).getItems().get(0).getProductName());
        assertEquals("50.00", result.getContent().get(0).getItems().get(0).getSubtotal().toString());
        verify(orderRepository).findAll(any(Pageable.class));
    }

    @Test
    void getAllOrders_filtersByEmailAndStatus() {
        when(orderRepository.findByUserEmailContainingIgnoreCaseAndStatus(eq("user@example.com"), eq(OrderStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        Page<OrderResponse> result = adminOrderService.getAllOrders(0, 10, "desc", OrderStatus.PENDING, " user@example.com ");

        assertEquals(1, result.getTotalElements());
        verify(orderRepository).findByUserEmailContainingIgnoreCaseAndStatus(eq("user@example.com"), eq(OrderStatus.PENDING), any(Pageable.class));
    }

    @Test
    void getAllOrders_filtersByEmailOnly() {
        when(orderRepository.findByUserEmailContainingIgnoreCase(eq("user"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        assertEquals(1, adminOrderService.getAllOrders(0, 10, "asc", null, "user").getTotalElements());
        verify(orderRepository).findByUserEmailContainingIgnoreCase(eq("user"), any(Pageable.class));
    }

    @Test
    void getAllOrders_filtersByStatusOnly() {
        when(orderRepository.findByStatus(eq(OrderStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        assertEquals(1, adminOrderService.getAllOrders(0, 10, "asc", OrderStatus.PENDING, " ").getTotalElements());
        verify(orderRepository).findByStatus(eq(OrderStatus.PENDING), any(Pageable.class));
    }

    @Test
    void getAllOrders_usesRequestedPagingAndSort() {
        when(orderRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());
        adminOrderService.getAllOrders(2, 15, "desc", null, null);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findAll(captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(15, captor.getValue().getPageSize());
        assertEquals("createdAt", captor.getValue().getSort().getOrderFor("createdAt").getProperty());
        assertTrue(captor.getValue().getSort().getOrderFor("createdAt").isDescending());
    }

    @Test void getAllOrders_rejectsNegativePage() {
        assertThrows(IllegalArgumentException.class, () -> adminOrderService.getAllOrders(-1, 10, "asc", null, null));
    }
    @Test void getAllOrders_rejectsZeroSize() {
        assertThrows(IllegalArgumentException.class, () -> adminOrderService.getAllOrders(0, 0, "asc", null, null));
    }
    @Test void getAllOrders_rejectsSizeAbove100() {
        assertThrows(IllegalArgumentException.class, () -> adminOrderService.getAllOrders(0, 101, "asc", null, null));
    }
    @Test void getAllOrders_rejectsInvalidDirection() {
        assertThrows(IllegalArgumentException.class, () -> adminOrderService.getAllOrders(0, 10, "sideways", null, null));
    }

    @Test
    void getOrder_returnsOrderResponse() {
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        OrderResponse result = adminOrderService.getOrder(1);
        assertEquals(1, result.getOrderId());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(1, result.getItems().size());
    }

    @Test
    void getOrder_throwsWhenMissing() {
        when(orderRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adminOrderService.getOrder(99));
    }

    @Test
    void updateOrderStatus_pendingToConfirmed_succeeds() {
        when(orderRepository.findByIdForUpdate(1)).thenReturn(Optional.of(order));
        OrderResponse result = adminOrderService.updateOrderStatus(1, new UpdateOrderStatusRequest(OrderStatus.CONFIRMED));
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        verify(productRepository, never()).findByIdForUpdate(anyInt());
    }

    @Test
    void updateOrderStatus_pendingToCancelled_restoresStock() {
        when(orderRepository.findByIdForUpdate(1)).thenReturn(Optional.of(order));
        when(productRepository.findByIdForUpdate(10)).thenReturn(Optional.of(product));
        OrderResponse result = adminOrderService.updateOrderStatus(1, new UpdateOrderStatusRequest(OrderStatus.CANCELLED));
        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        assertEquals(7, product.getStock());
    }

    @Test
    void updateOrderStatus_throwsWhenOrderMissing() {
        when(orderRepository.findByIdForUpdate(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adminOrderService.updateOrderStatus(99, new UpdateOrderStatusRequest(OrderStatus.CONFIRMED)));
    }

    @Test
    void updateOrderStatus_rejectsPendingToShipped() {
        when(orderRepository.findByIdForUpdate(1)).thenReturn(Optional.of(order));
        assertThrows(InvalidOrderStatusException.class, () -> adminOrderService.updateOrderStatus(1, new UpdateOrderStatusRequest(OrderStatus.SHIPPED)));
    }

    @Test
    void updateOrderStatus_rejectsDeliveredTransition() {
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findByIdForUpdate(1)).thenReturn(Optional.of(order));
        assertThrows(InvalidOrderStatusException.class, () -> adminOrderService.updateOrderStatus(1, new UpdateOrderStatusRequest(OrderStatus.CANCELLED)));
    }

    @Test
    void updateOrderStatus_confirmedToShipped_succeeds() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findByIdForUpdate(1)).thenReturn(Optional.of(order));
        assertEquals(OrderStatus.SHIPPED, adminOrderService.updateOrderStatus(1, new UpdateOrderStatusRequest(OrderStatus.SHIPPED)).getStatus());
    }

    @Test
    void updateOrderStatus_shippedToDelivered_succeeds() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findByIdForUpdate(1)).thenReturn(Optional.of(order));
        assertEquals(OrderStatus.DELIVERED, adminOrderService.updateOrderStatus(1, new UpdateOrderStatusRequest(OrderStatus.DELIVERED)).getStatus());
    }

    @Test
    void updateOrderStatus_cancelledOrderCannotBeChanged() {
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findByIdForUpdate(1)).thenReturn(Optional.of(order));
        assertThrows(InvalidOrderStatusException.class, () -> adminOrderService.updateOrderStatus(1, new UpdateOrderStatusRequest(OrderStatus.CONFIRMED)));
    }

    @Test
    void updateOrderStatus_throwsWhenProductMissingDuringCancellation() {
        when(orderRepository.findByIdForUpdate(1)).thenReturn(Optional.of(order));
        when(productRepository.findByIdForUpdate(10)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adminOrderService.updateOrderStatus(1, new UpdateOrderStatusRequest(OrderStatus.CANCELLED)));
    }
}
