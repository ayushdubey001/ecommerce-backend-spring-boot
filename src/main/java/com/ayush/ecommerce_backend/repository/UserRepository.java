package com.ayush.ecommerce_backend.repository;

import com.ayush.ecommerce_backend.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Integer> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(
            String email,
            Integer id
    );

    Optional<Users> findByEmail(String email);

    Page<Users> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );

    Page<Users> findByEmailContainingIgnoreCase(
            String email,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(u)
            FROM Users u
            JOIN u.roleList r
            WHERE r.name = :roleName
            AND u.enabled = true
            """)
    long countEnabledUsersByRole(
            @Param("roleName") String roleName
    );
}