package com.example.invoicetracker.controller;

import com.example.invoicetracker.dto.CategoryFilterRequest;
import com.example.invoicetracker.dto.CategoryRequest;
import com.example.invoicetracker.dto.CategoryResponse;
import com.example.invoicetracker.service.CategoryService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    @PreAuthorize("hasAuthority('ROLE_SUPERUSER')")
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@RequestBody @Validated CategoryRequest request) {

        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PreAuthorize("hasAuthority('ROLE_SUPERUSER')")
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id,
            @RequestBody @Validated CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @PreAuthorize("hasAuthority('ROLE_SUPERUSER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<CategoryResponse>> listCategories(CategoryFilterRequest filter) {
        return ResponseEntity.ok(categoryService.listCategories(filter));
    }

}