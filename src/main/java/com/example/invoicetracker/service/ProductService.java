package com.example.invoicetracker.service;

import com.example.invoicetracker.dto.ProductFilterRequest;
import com.example.invoicetracker.dto.ProductRequest;
import com.example.invoicetracker.dto.ProductResponse;
import com.example.invoicetracker.exception.ResourceAlreadyExistsException;
import com.example.invoicetracker.exception.ResourceNotFoundException;
import com.example.invoicetracker.model.entity.Product;
import com.example.invoicetracker.repository.CategoryRepository;
import com.example.invoicetracker.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        boolean codeExistsActive = productRepository.findByProductCodeAndIsActiveTrue(request.getProductCode())
                .isPresent();
        boolean nameExistsActive = productRepository.findByProductNameAndIsActiveTrue(request.getProductName())
                .isPresent();

        if (codeExistsActive || nameExistsActive) {
            throw new ResourceAlreadyExistsException("Product code or name already exists");
        }

        productRepository.findByProductCode(request.getProductCode())
                .filter(p -> p.getIsActive() != null && !p.getIsActive())
                .ifPresentOrElse(inactive -> {
                    inactive.setProductName(request.getProductName());
                    inactive.setCategory(categoryRepository.findById(request.getCategoryId())
                            .orElseThrow(() -> new ResourceNotFoundException("Category not found")));
                    inactive.setDescription(request.getDescription());
                    inactive.setUnitPrice(request.getUnitPrice());
                    inactive.setIsActive(true);
                    productRepository.save(inactive);
                }, () -> {
                    Product product = Product.builder()
                            .productCode(request.getProductCode())
                            .productName(request.getProductName())
                            .category(categoryRepository.findById(request.getCategoryId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Category not found")))
                            .description(request.getDescription())
                            .unitPrice(request.getUnitPrice())
                            .build();

                    product.setIsActive(true);
                    productRepository.save(product);

                });

        Product saved = productRepository.findByProductCodeAndIsActiveTrue(request.getProductCode())
                .orElseThrow(() -> new RuntimeException("Failed to create or reactivate product"));

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .filter(Product::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getIsActive() == null || !product.getIsActive()) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }

        if (!product.getProductCode().equals(request.getProductCode())) {
            productRepository.findByProductCodeAndIsActiveTrue(request.getProductCode())
                    .ifPresent(p -> {
                        throw new ResourceAlreadyExistsException("Product code already exists");
                    });
        }

        if (!product.getProductName().equals(request.getProductName())) {
            productRepository.findByProductNameAndIsActiveTrue(request.getProductName())
                    .ifPresent(p -> {
                        throw new ResourceAlreadyExistsException("Product name already exists");
                    });
        }

        product.setProductCode(request.getProductCode());
        product.setProductName(request.getProductName());
        product.setCategory(categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found")));
        product.setDescription(request.getDescription());
        product.setUnitPrice(request.getUnitPrice());

        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setIsActive(false);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(ProductFilterRequest filter) {
        Sort sort = Sort.by(Sort.Direction.fromString(filter.getDirection()), filter.getSortBy());
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Page<Product> result;
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            result = productRepository.searchActiveProducts(filter.getSearch(), pageable);
        } else {
            result = productRepository.findAllByIsActiveTrue(pageable);
        }

        return result.map(this::mapToResponse);
    }

    private ProductResponse mapToResponse(Product p) {
        return ProductResponse.builder()
                .productId(p.getProductId())
                .categoryId(p.getCategory().getCategoryId())
                .categoryName(p.getCategory().getCategoryName())
                .productCode(p.getProductCode())
                .productName(p.getProductName())
                .description(p.getDescription())
                .unitPrice(p.getUnitPrice())
                .isActive(p.getIsActive())
                .build();
    }
}
