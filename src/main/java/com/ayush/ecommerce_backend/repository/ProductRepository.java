package com.ayush.ecommerce_backend.repository;

import com.ayush.ecommerce_backend.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    @EntityGraph(attributePaths = "category")
    Page<Product> findByCategoryId(
            Integer categoryId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "category")
    Page<Product> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "category")
    Page<Product> findByNameContainingIgnoreCaseAndCategoryId(
            String name,
            Integer categoryId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findProductById(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Product p
            WHERE p.id = :id
            """)
    Optional<Product> findByIdForUpdate(
            @Param("id") Integer id
    );

    boolean existsByCategoryId(Integer categoryId);
}