package com.ayush.ecommerce_backend.controller;

import com.ayush.ecommerce_backend.dto.CategoryRequest;
import com.ayush.ecommerce_backend.dto.CategoryResponse;
import com.ayush.ecommerce_backend.entity.Category;
import com.ayush.ecommerce_backend.service.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;


@Tag(
        name = "Categories",
        description = "APIs for managing product categories"
)
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping()
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest categoryRequest){
        CategoryResponse categoryResponse = categoryService.createCategory(categoryRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                        .body(categoryResponse);

    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories(){
        List<CategoryResponse> allCategories=categoryService.getAllCategories();
        return ResponseEntity.status(HttpStatus.FOUND)
                .body(allCategories);
    }


    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable Integer id){
        CategoryResponse categoryResponse = categoryService.getCategory(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(categoryResponse);
    }


    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Integer id,
                                                  @Valid @RequestBody CategoryRequest categoryRequest){
        CategoryResponse categoryResponse = categoryService.updateCategory(id,categoryRequest);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(categoryResponse);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteCategory(@PathVariable Integer id){
       categoryService.deleteCategory(id);
        return ResponseEntity.status(HttpStatus.OK)
                .build();


    }




}
