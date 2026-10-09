package com.ayush.ecommerce_backend.repository;

import com.ayush.ecommerce_backend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem,Integer> {
    boolean existsByProductId(Integer productId);
}
