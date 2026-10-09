
package com.ayush.ecommerce_backend.service;

import com.ayush.ecommerce_backend.dto.CategoryRequest;
import com.ayush.ecommerce_backend.dto.CategoryResponse;
import com.ayush.ecommerce_backend.entity.Category;
import com.ayush.ecommerce_backend.exception.ConflictException;
import com.ayush.ecommerce_backend.exception.DuplicateResourceException;
import com.ayush.ecommerce_backend.exception.ResourceNotFoundException;
import com.ayush.ecommerce_backend.repository.CategoryRepository;
import com.ayush.ecommerce_backend.repository.ProductRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void createCategory_shouldCreateCategorySuccessfully() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Electronics");

        Category savedCategory = new Category(1, "Electronics");

        when(categoryRepository.existsByName("Electronics"))
                .thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenReturn(savedCategory);

        CategoryResponse response =
                categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("Electronics", response.getName());

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_shouldRejectDuplicateName() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Electronics");

        when(categoryRepository.existsByName("Electronics"))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> categoryService.createCategory(request)
        );

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void getAllCategories_shouldReturnAllCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(
                new Category(1, "Electronics"),
                new Category(2, "Books")
        ));

        List<CategoryResponse> responses =
                categoryService.getAllCategories();

        assertEquals(2, responses.size());
        assertEquals("Electronics", responses.get(0).getName());
        assertEquals("Books", responses.get(1).getName());
    }

    @Test
    void getAllCategories_shouldReturnEmptyListWhenNoCategoriesExist() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        List<CategoryResponse> responses =
                categoryService.getAllCategories();

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void getCategory_shouldReturnCategoryWhenFound() {
        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(new Category(1, "Electronics")));

        CategoryResponse response = categoryService.getCategory(1);

        assertEquals(1, response.getId());
        assertEquals("Electronics", response.getName());
    }

    @Test
    void getCategory_shouldThrowWhenCategoryDoesNotExist() {
        when(categoryRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.getCategory(99)
        );
    }

    @Test
    void updateCategory_shouldUpdateCategorySuccessfully() {
        Category existingCategory = new Category(1, "Electronics");

        CategoryRequest request = new CategoryRequest();
        request.setName("Gadgets");

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsByNameAndIdNot("Gadgets", 1))
                .thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response =
                categoryService.updateCategory(1, request);

        assertEquals(1, response.getId());
        assertEquals("Gadgets", response.getName());

        verify(categoryRepository).save(existingCategory);
    }

    @Test
    void updateCategory_shouldRejectDuplicateName() {
        Category existingCategory = new Category(1, "Electronics");

        CategoryRequest request = new CategoryRequest();
        request.setName("Books");

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsByNameAndIdNot("Books", 1))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> categoryService.updateCategory(1, request)
        );

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_shouldThrowWhenCategoryDoesNotExist() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Gadgets");

        when(categoryRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.updateCategory(99, request)
        );

        verify(categoryRepository, never())
                .save(any(Category.class));
    }

    @Test
    void deleteCategory_shouldDeleteWhenNoProductsAreAssociated() {
        Category category = new Category(1, "Electronics");

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryId(1))
                .thenReturn(false);

        categoryService.deleteCategory(1);

        verify(categoryRepository).deleteById(1);
    }

    @Test
    void deleteCategory_shouldRejectWhenProductsAreAssociated() {
        Category category = new Category(1, "Electronics");

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryId(1))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> categoryService.deleteCategory(1)
        );

        verify(categoryRepository, never()).deleteById(1);
    }

    @Test
    void deleteCategory_shouldThrowWhenCategoryDoesNotExist() {
        when(categoryRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.deleteCategory(99)
        );

        verify(categoryRepository, never()).deleteById(anyInt());
    }
}
