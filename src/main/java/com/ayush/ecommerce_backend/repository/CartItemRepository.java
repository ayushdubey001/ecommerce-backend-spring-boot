package com.ayush.ecommerce_backend.repository;

import com.ayush.ecommerce_backend.entity.Cart;
import com.ayush.ecommerce_backend.entity.CartItem;
import com.ayush.ecommerce_backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    List<CartItem> findByCart(Cart cart);
}

