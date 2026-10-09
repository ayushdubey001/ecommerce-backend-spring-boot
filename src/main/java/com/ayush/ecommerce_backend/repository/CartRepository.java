package com.ayush.ecommerce_backend.repository;

import com.ayush.ecommerce_backend.entity.Cart;
import com.ayush.ecommerce_backend.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    Optional<Cart> findByUser(Users users);
}
