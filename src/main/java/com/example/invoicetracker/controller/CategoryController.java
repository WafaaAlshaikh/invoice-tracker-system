package com.example.invoicetracker.controller;

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
        System.out.println("POST /categories called!");
        System.out.println("Category Code: " + request.getCategoryCode());
        System.out.println("Category Name: " + request.getCategoryName());
        System.out.println("Description: " + request.getDescription());

        CategoryResponse response = categoryService.createCategory(request);
        System.out.println("Category saved with ID: " + response.getCategoryId());
        
        return ResponseEntity.status(201).body(response);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_SUPERUSER','ROLE_USER','ROLE_AUDITOR')") 
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PreAuthorize("hasAuthority('ROLE_SUPERUSER')") 
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id, @RequestBody @Validated CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @PreAuthorize("hasAuthority('ROLE_SUPERUSER')")  
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_SUPERUSER','ROLE_USER',ROLE_AUDITOR')")  
    @GetMapping
    public ResponseEntity<Page<CategoryResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "categoryId") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String search
    ) {
        Page<CategoryResponse> result = categoryService.listCategories(page, size, sortBy, direction, search);
        return ResponseEntity.ok(result);
    }
}