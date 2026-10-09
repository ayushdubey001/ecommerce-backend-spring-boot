package com.ayush.ecommerce_backend.repository;

import com.ayush.ecommerce_backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category,Integer> {

    boolean existsByName(String newCategory);
    boolean existsByNameAndIdNot(String name, Integer id);
}
