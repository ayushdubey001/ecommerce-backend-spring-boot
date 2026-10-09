
package com.ayush.ecommerce_backend.service;

import com.ayush.ecommerce_backend.dto.ProductRequest;
import com.ayush.ecommerce_backend.dto.ProductResponse;
import com.ayush.ecommerce_backend.entity.Category;
import com.ayush.ecommerce_backend.entity.Product;
import com.ayush.ecommerce_backend.enums.Availability;
import com.ayush.ecommerce_backend.exception.ConflictException;
import com.ayush.ecommerce_backend.exception.ResourceNotFoundException;
import com.ayush.ecommerce_backend.repository.CategoryRepository;
import com.ayush.ecommerce_backend.repository.OrderItemRepository;
import com.ayush.ecommerce_backend.repository.ProductRepository;

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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ProductService productService;

    private Category createCategory() {
        return new Category(1, "Electronics");
    }

    private Product createProduct() {
        Product product = new Product();
        product.setId(1);
        product.setName("Keyboard");
        product.setDescription("Mechanical keyboard");
        product.setPrice(new BigDecimal("1500.00"));
        product.setStock(10);
        product.setCategory(createCategory());
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return product;
    }

    private ProductRequest createRequest() {
        ProductRequest request = new ProductRequest();
        request.setName("Keyboard");
        request.setDescription("Mechanical keyboard");
        request.setPrice(new BigDecimal("1500.00"));
        request.setStock(10);
        request.setCategoryId(1);
        return request;
    }

    @Test
    void createProduct_shouldCreateProductSuccessfully() {
        ProductRequest request = createRequest();

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(createCategory()));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product product = invocation.getArgument(0);
                    product.setId(1);
                    return product;
                });

        ProductResponse response =
                productService.createProduct(request);

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("Keyboard", response.getName());
        assertEquals(new BigDecimal("1500.00"), response.getPrice());
        assertEquals("Electronics", response.getCategory());
        assertEquals(Availability.IN_STOCK, response.getAvailability());

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_shouldThrowWhenCategoryDoesNotExist() {
        ProductRequest request = createRequest();

        when(categoryRepository.findById(1))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.createProduct(request)
        );

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getProduct_shouldReturnProductWhenFound() {
        when(productRepository.findProductById(1))
                .thenReturn(Optional.of(createProduct()));

        ProductResponse response = productService.getProduct(1);

        assertEquals(1, response.getId());
        assertEquals("Keyboard", response.getName());
        assertEquals("Electronics", response.getCategory());
        assertEquals(Availability.IN_STOCK, response.getAvailability());
    }

    @Test
    void getProduct_shouldThrowWhenProductDoesNotExist() {
        when(productRepository.findProductById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getProduct(99)
        );
    }

    @Test
    void getAllProducts_shouldReturnPagedProducts() {
        Product product = createProduct();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage =
                new PageImpl<>(List.of(product), pageable, 1);

        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(productPage);

        Page<ProductResponse> response =
                productService.getAllProducts(
                        0, 10, "name", "asc", null, null
                );

        assertEquals(1, response.getTotalElements());
        assertEquals("Keyboard", response.getContent().get(0).getName());
        assertEquals("Electronics",
                response.getContent().get(0).getCategory());
    }

    @Test
    void getAllProducts_shouldRejectNegativePage() {
        assertThrows(
                IllegalArgumentException.class,
                () -> productService.getAllProducts(
                        -1, 10, "name", "asc", null, null
                )
        );
    }

    @Test
    void getAllProducts_shouldRejectInvalidPageSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> productService.getAllProducts(
                        0, 101, "name", "asc", null, null
                )
        );
    }

    @Test
    void getAllProducts_shouldRejectInvalidSortField() {
        assertThrows(
                IllegalArgumentException.class,
                () -> productService.getAllProducts(
                        0, 10, "password", "asc", null, null
                )
        );
    }

    @Test
    void getAllProducts_shouldRejectInvalidSortDirection() {
        assertThrows(
                IllegalArgumentException.class,
                () -> productService.getAllProducts(
                        0, 10, "name", "sideways", null, null
                )
        );
    }

    @Test
    void getAllProducts_shouldSearchByName() {
        Product product = createProduct();

        Pageable pageable = PageRequest.of(
                0, 10,
                org.springframework.data.domain.Sort.by("name").ascending()
        );

        when(productRepository.findByNameContainingIgnoreCase(
                eq("Keyboard"), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(product), pageable, 1));

        Page<ProductResponse> response =
                productService.getAllProducts(
                        0, 10, "name", "asc", null, " Keyboard "
                );

        assertEquals(1, response.getTotalElements());

        verify(productRepository).findByNameContainingIgnoreCase(
                eq("Keyboard"), any(Pageable.class)
        );
    }

    @Test
    void getAllProducts_shouldFilterByCategory() {
        Product product = createProduct();

        when(productRepository.findByCategoryId(
                eq(1), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductResponse> response =
                productService.getAllProducts(
                        0, 10, "name", "asc", 1, null
                );

        assertEquals(1, response.getTotalElements());

        verify(productRepository).findByCategoryId(
                eq(1), any(Pageable.class)
        );
    }

    @Test
    void getAllProducts_shouldSearchByNameAndCategory() {
        Product product = createProduct();

        when(productRepository.findByNameContainingIgnoreCaseAndCategoryId(
                eq("Keyboard"), eq(1), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductResponse> response =
                productService.getAllProducts(
                        0, 10, "name", "asc", 1, "Keyboard"
                );

        assertEquals(1, response.getTotalElements());

        verify(productRepository)
                .findByNameContainingIgnoreCaseAndCategoryId(
                        eq("Keyboard"), eq(1), any(Pageable.class)
                );
    }

    @Test
    void updateProduct_shouldUpdateExistingProduct() {
        Product existingProduct = createProduct();
        ProductRequest request = createRequest();
        request.setName("Gaming Keyboard");
        request.setStock(20);

        when(productRepository.findById(1))
                .thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(createCategory()));

        ProductResponse response =
                productService.updateProduct(1, request);

        assertEquals("Gaming Keyboard", response.getName());
        assertEquals(20, response.getStock());
        assertEquals(Availability.IN_STOCK, response.getAvailability());

        verify(productRepository).findById(1);
    }

    @Test
    void updateProduct_shouldThrowWhenProductDoesNotExist() {
        when(productRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.updateProduct(99, createRequest())
        );
    }

    @Test
    void updateProduct_shouldThrowWhenCategoryDoesNotExist() {
        when(productRepository.findById(1))
                .thenReturn(Optional.of(createProduct()));
        when(categoryRepository.findById(99))
                .thenReturn(Optional.empty());

        ProductRequest request = createRequest();
        request.setCategoryId(99);

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.updateProduct(1, request)
        );
    }

    @Test
    void deleteProduct_shouldDeleteProductWithoutOrderHistory() {
        when(productRepository.findById(1))
                .thenReturn(Optional.of(createProduct()));
        when(orderItemRepository.existsByProductId(1))
                .thenReturn(false);

        productService.deleteProduct(1);

        verify(productRepository).delete(any(Product.class));
    }

    @Test
    void deleteProduct_shouldRejectProductWithOrderHistory() {
        when(productRepository.findById(1))
                .thenReturn(Optional.of(createProduct()));
        when(orderItemRepository.existsByProductId(1))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> productService.deleteProduct(1)
        );

        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void deleteProduct_shouldThrowWhenProductDoesNotExist() {
        when(productRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.deleteProduct(99)
        );

        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void getProduct_shouldReturnOutOfStockWhenStockIsZero() {
        Product product = createProduct();
        product.setStock(0);

        when(productRepository.findProductById(1))
                .thenReturn(Optional.of(product));

        ProductResponse response = productService.getProduct(1);

        assertEquals(Availability.OUT_OF_STOCK, response.getAvailability());
    }
}
