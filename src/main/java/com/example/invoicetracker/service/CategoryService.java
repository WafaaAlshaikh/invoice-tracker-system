package com.example.invoicetracker.service;

import com.example.invoicetracker.dto.CategoryFilterRequest;
import com.example.invoicetracker.dto.CategoryRequest;
import com.example.invoicetracker.dto.CategoryResponse;
import com.example.invoicetracker.exception.ResourceAlreadyExistsException;
import com.example.invoicetracker.exception.ResourceNotFoundException;
import com.example.invoicetracker.model.entity.Category;
import com.example.invoicetracker.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        boolean codeExistsActive = categoryRepository.findByCategoryCodeAndIsActiveTrue(request.getCategoryCode())
                .isPresent();
        boolean nameExistsActive = categoryRepository.findByCategoryNameAndIsActiveTrue(request.getCategoryName())
                .isPresent();

        if (codeExistsActive || nameExistsActive) {
            throw new ResourceAlreadyExistsException("Category code or name already exists");
        }

        categoryRepository.findByCategoryCode(request.getCategoryCode())
                .filter(c -> c.getIsActive() != null && !c.getIsActive())
                .ifPresentOrElse(inactive -> {
                    inactive.setCategoryName(request.getCategoryName());
                    inactive.setDescription(request.getDescription());
                    inactive.setIsActive(true);
                    categoryRepository.save(inactive);
                }, () -> {

                    Category category = Category.builder()
                            .categoryCode(request.getCategoryCode())
                            .categoryName(request.getCategoryName())
                            .description(request.getDescription())
                            .build();
                    categoryRepository.save(category);
                });

        Category saved = categoryRepository.findByCategoryCodeAndIsActiveTrue(request.getCategoryCode())
                .orElseThrow(() -> new RuntimeException("Failed to create or reactivate category"));

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {

        Category category = categoryRepository.findByCategoryIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return mapToResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (category.getIsActive() == null || !category.getIsActive()) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }

        if (!category.getCategoryCode().equals(request.getCategoryCode())) {
            categoryRepository.findByCategoryCodeAndIsActiveTrue(request.getCategoryCode())
                    .ifPresent(c -> {
                        throw new ResourceAlreadyExistsException("Category code already exists");
                    });
        }

        if (!category.getCategoryName().equals(request.getCategoryName())) {
            categoryRepository.findByCategoryNameAndIsActiveTrue(request.getCategoryName())
                    .ifPresent(c -> {
                        throw new ResourceAlreadyExistsException("Category name already exists");
                    });
        }

        category.setCategoryCode(request.getCategoryCode());
        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());

        Category updated = categoryRepository.save(category);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        category.setIsActive(false);
        categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> listCategories(CategoryFilterRequest filter) {
        Sort sort = Sort.by(Sort.Direction.fromString(filter.getDirection()), filter.getSortBy());
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Page<Category> result;

        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            result = categoryRepository.searchActiveCategories(filter.getSearch(), pageable);
        } else {
            result = categoryRepository.findAllByIsActiveTrue(pageable);
        }

        return result.map(this::mapToResponse);
    }

    private CategoryResponse mapToResponse(Category c) {
        return CategoryResponse.builder()
                .categoryId(c.getCategoryId())
                .categoryCode(c.getCategoryCode())
                .categoryName(c.getCategoryName())
                .description(c.getDescription())
                .isActive(c.getIsActive())
                .build();
    }
}
