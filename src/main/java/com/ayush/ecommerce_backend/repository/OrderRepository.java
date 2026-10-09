package com.ayush.ecommerce_backend.repository;

import com.ayush.ecommerce_backend.entity.Order;
import com.ayush.ecommerce_backend.entity.Users;
import com.ayush.ecommerce_backend.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    Page<Order> findByUser(
            Users user,
            Pageable pageable
    );

    Page<Order> findByUserAndStatus(
            Users user,
            OrderStatus status,
            Pageable pageable
    );

    Page<Order> findByStatus(
            OrderStatus status,
            Pageable pageable
    );

    Page<Order> findByUserEmailContainingIgnoreCase(
            String email,
            Pageable pageable
    );

    Page<Order> findByUserEmailContainingIgnoreCaseAndStatus(
            String email,
            OrderStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM Order o
            WHERE o.id = :id
            """)
    Optional<Order> findByIdForUpdate(
            @Param("id") Integer id
    );
}