package com.ayush.ecommerce_backend.service;

import com.ayush.ecommerce_backend.dto.ProductRequest;
import com.ayush.ecommerce_backend.dto.ProductResponse;
import com.ayush.ecommerce_backend.entity.Category;
import com.ayush.ecommerce_backend.entity.Product;
import com.ayush.ecommerce_backend.exception.ConflictException;
import com.ayush.ecommerce_backend.exception.ResourceNotFoundException;
import com.ayush.ecommerce_backend.repository.CategoryRepository;
import com.ayush.ecommerce_backend.repository.OrderItemRepository;
import com.ayush.ecommerce_backend.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;


    // CREATE PRODUCT
    public ProductResponse createProduct(ProductRequest request) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with id " + request.getCategoryId()
                        )
                );

        Product product = new Product();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(category);

        LocalDateTime now = LocalDateTime.now();

        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        Product savedProduct = productRepository.save(product);

        return entityToResponse(savedProduct);
    }

    // GET PRODUCT BY ID
    public ProductResponse getProduct(Integer id) {

        Product product = productRepository.findProductById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id " + id
                        )
                );

        return entityToResponse(product);
    }


    // GET PRODUCTS
    // SEARCH + CATEGORY + PAGINATION + SORTING
    public Page<ProductResponse> getAllProducts(
            int page,
            int size,
            String sortBy,
            String direction,
            Integer categoryId,
            String search
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


        // SAFE SORTING
        List<String> allowedSortFields = List.of(
                "id",
                "name",
                "price",
                "stock",
                "createdAt",
                "updatedAt"
        );

        if (!allowedSortFields.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sort field. Allowed fields: "
                            + allowedSortFields
            );
        }

        Sort.Direction sortDirection;

        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid sort direction. Use 'asc' or 'desc'"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        Page<Product> products;

        boolean hasSearch =
                search != null && !search.trim().isEmpty();

        boolean hasCategory =
                categoryId != null;



        // SEARCH + CATEGORY
        if (hasSearch && hasCategory) {

            products =
                    productRepository
                            .findByNameContainingIgnoreCaseAndCategoryId(
                                    search.trim(),
                                    categoryId,
                                    pageable
                            );
        }


        // SEARCH ONLY
        else if (hasSearch) {

            products =
                    productRepository
                            .findByNameContainingIgnoreCase(
                                    search.trim(),
                                    pageable
                            );
        }

        // CATEGORY ONLY
        else if (hasCategory) {

            products =
                    productRepository
                            .findByCategoryId(
                                    categoryId,
                                    pageable
                            );
        }


        // ALL PRODUCTS
        else {

            products = productRepository.findAll(pageable);
        }

        return products.map(this::entityToResponse);
    }



    // UPDATE PRODUCT
    @Transactional
    public ProductResponse updateProduct(
            Integer id,
            ProductRequest request
    ) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id " + id
                        )
                );

        Category category = categoryRepository.findById(
                        request.getCategoryId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with id "
                                        + request.getCategoryId()
                        )
                );

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(category);
        product.setUpdatedAt(LocalDateTime.now());

        return entityToResponse(product);
    }


    // DELETE PRODUCT
    @Transactional
    public void deleteProduct(Integer id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id " + id
                        )
                );
        if (orderItemRepository.existsByProductId(id)) {

            throw new ConflictException(
                    "Product cannot be deleted because it exists in order history"
            );
        }

        productRepository.delete(product);
    }

    // ENTITY -> RESPONSE
    private ProductResponse entityToResponse(Product product) {

        ProductResponse response = new ProductResponse();

        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setCategory(product.getCategory().getName());
        response.setStock(product.getStock());

        if (product.getStock() != null && product.getStock() > 0) {
            response.setAvailability(
                    com.ayush.ecommerce_backend.enums.Availability.IN_STOCK
            );
        } else {
            response.setAvailability(
                    com.ayush.ecommerce_backend.enums.Availability.OUT_OF_STOCK
            );
        }

        response.setCreatedAt(product.getCreatedAt());

        return response;
    }
}