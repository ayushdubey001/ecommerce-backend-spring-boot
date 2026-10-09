package com.ayush.ecommerce_backend.service;

import com.ayush.ecommerce_backend.dto.CategoryRequest;
import com.ayush.ecommerce_backend.dto.CategoryResponse;
import com.ayush.ecommerce_backend.entity.Category;
import com.ayush.ecommerce_backend.exception.ConflictException;
import com.ayush.ecommerce_backend.exception.DuplicateResourceException;
import com.ayush.ecommerce_backend.exception.ResourceNotFoundException;
import com.ayush.ecommerce_backend.repository.CategoryRepository;
import com.ayush.ecommerce_backend.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;


@Service
@AllArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryResponse createCategory(CategoryRequest categoryRequest){
         boolean isExists = categoryRepository.existsByName(categoryRequest.getName());
         if(isExists){
            throw new DuplicateResourceException(categoryRequest.getName() + " Already Exists");
         }
         Category category = new Category();
         category.setName(categoryRequest.getName());
         return entityToCategoryResponse(categoryRepository.save(category));
    }



    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::entityToCategoryResponse)
                .toList();
    }

    public CategoryResponse getCategory(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(()->
                        new ResourceNotFoundException("Category with id" + id + " not found!!"));

        return entityToCategoryResponse(category);
    }

    public CategoryResponse updateCategory(Integer id, CategoryRequest categoryRequest) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(()->
                        new ResourceNotFoundException("Category with id" + id + " not found!!"));

        if(categoryRepository.existsByNameAndIdNot(categoryRequest.getName() ,id)){
            throw new DuplicateResourceException("Category already exists!!");
        }

        category.setName(categoryRequest.getName());
        categoryRepository.save(category);
        return entityToCategoryResponse(category);
    }

    public void deleteCategory(Integer id) {
       Category category = categoryRepository.findById(id)
               .orElseThrow(()->
                       new ResourceNotFoundException("Category with id" + id + " not found!!"));

        if (productRepository.existsByCategoryId(id)) {

            throw new ConflictException(
                    "Category cannot be deleted because products are associated with it"
            );
        }
        categoryRepository.deleteById(id);
    }

    private CategoryResponse entityToCategoryResponse(Category category) {
        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setId(category.getId());
        categoryResponse.setName(category.getName());
        return categoryResponse;
    }






}
