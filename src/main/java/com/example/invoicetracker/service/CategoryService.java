package com.example.invoicetracker.service;

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

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByCategoryCode(request.getCategoryCode())) {
            throw new ResourceAlreadyExistsException("Category code already exists");
        }
        if (categoryRepository.existsByCategoryName(request.getCategoryName())) {
            throw new ResourceAlreadyExistsException("Category name already exists");
        }

        Category category = Category.builder()
                .categoryCode(request.getCategoryCode())
                .categoryName(request.getCategoryName())
                .description(request.getDescription())
                .build();

        Category saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return mapToResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (!category.getCategoryCode().equals(request.getCategoryCode()) &&
                categoryRepository.existsByCategoryCode(request.getCategoryCode())) {
            throw new ResourceAlreadyExistsException("Category code already exists");
        }
        if (!category.getCategoryName().equals(request.getCategoryName()) &&
                categoryRepository.existsByCategoryName(request.getCategoryName())) {
            throw new ResourceAlreadyExistsException("Category name already exists");
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
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> listCategories(int page, int size, String sortBy, String direction, String search) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Category> result;
        if (search != null && !search.isBlank()) {
            result = categoryRepository.findAll(pageable)
                    .map(c -> c); 
                      result = new PageImpl<>(
                    categoryRepository.findAll().stream()
                            .filter(c -> c.getCategoryCode().toLowerCase().contains(search.toLowerCase())
                                    || c.getCategoryName().toLowerCase().contains(search.toLowerCase()))
                            .collect(Collectors.toList()),
                    pageable,
                    categoryRepository.findAll().stream()
                            .filter(c -> c.getCategoryCode().toLowerCase().contains(search.toLowerCase())
                                    || c.getCategoryName().toLowerCase().contains(search.toLowerCase()))
                            .count()
            );
        } else {
            result = categoryRepository.findAll(pageable);
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
