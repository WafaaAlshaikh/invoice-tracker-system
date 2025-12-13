package com.example.invoicetracker.service;

import com.example.invoicetracker.dto.ProductFilterRequest;
import com.example.invoicetracker.dto.ProductRequest;
import com.example.invoicetracker.dto.ProductResponse;
import com.example.invoicetracker.exception.ResourceAlreadyExistsException;
import com.example.invoicetracker.exception.ResourceNotFoundException;
import com.example.invoicetracker.model.entity.Category;
import com.example.invoicetracker.model.entity.Product;
import com.example.invoicetracker.repository.CategoryRepository;
import com.example.invoicetracker.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

        @Mock
        private ProductRepository productRepository;

        @Mock
        private CategoryRepository categoryRepository;

        @InjectMocks
        private ProductService productService;

        @Captor
        private ArgumentCaptor<Product> productCaptor;

        private Category category;
        private ProductRequest productRequest;
        private Product product;

        @BeforeEach
        void setup() {
                category = Category.builder()
                                .categoryId(1L)
                                .categoryName("Electronics")
                                .build();

                productRequest = ProductRequest.builder()
                                .categoryId(1L)
                                .productCode("LAP001")
                                .productName("Gaming Laptop")
                                .description("High-performance gaming laptop")
                                .unitPrice(999.0)
                                .build();

                product = Product.builder()
                                .productId(1L)
                                .category(category)
                                .productCode("LAP001")
                                .productName("Gaming Laptop")
                                .description("High-performance gaming laptop")
                                .unitPrice(999.0)
                                .isActive(true)
                                .build();
        }

        @Nested
        class CreateProductTests {
                @Test
                void createProduct_success_newProduct() {
                        // Given
                        when(productRepository.findByProductCodeAndIsActiveTrue("LAP001"))
                                        .thenReturn(Optional.empty())
                                        .thenReturn(Optional.of(product));

                        when(productRepository.findByProductNameAndIsActiveTrue("Gaming Laptop"))
                                        .thenReturn(Optional.empty());

                        when(productRepository.findByProductCode("LAP001"))
                                        .thenReturn(Optional.empty());

                        when(categoryRepository.findById(1L))
                                        .thenReturn(Optional.of(category));

                        Product savedProduct = Product.builder()
                                        .productId(1L)
                                        .category(category)
                                        .productCode("LAP001")
                                        .productName("Gaming Laptop")
                                        .description("High-performance gaming laptop")
                                        .unitPrice(999.0)
                                        .isActive(true)
                                        .build();

                        when(productRepository.save(any(Product.class)))
                                        .thenReturn(savedProduct);

                        // When
                        ProductResponse response = productService.createProduct(productRequest);

                        // Then
                        assertNotNull(response);
                        assertEquals(1L, response.getProductId());
                        assertEquals("LAP001", response.getProductCode());
                        assertTrue(response.getIsActive());
                }

                @Test
                void createProduct_success_reactivateInactiveProduct() {
                        // Given
                        Product inactiveProduct = Product.builder()
                                        .productId(2L)
                                        .productCode("LAP001")
                                        .productName("Old Laptop Name")
                                        .description("Old description")
                                        .unitPrice(500.0)
                                        .isActive(false)
                                        .build();

                        when(productRepository.findByProductCodeAndIsActiveTrue("LAP001"))
                                        .thenReturn(Optional.empty())
                                        .thenReturn(Optional.of(inactiveProduct));

                        when(productRepository.findByProductNameAndIsActiveTrue("Gaming Laptop"))
                                        .thenReturn(Optional.empty());

                        when(productRepository.findByProductCode("LAP001"))
                                        .thenReturn(Optional.of(inactiveProduct));

                        when(categoryRepository.findById(1L))
                                        .thenReturn(Optional.of(category));

                        when(productRepository.save(any(Product.class)))
                                        .thenAnswer(invocation -> {
                                                Product p = invocation.getArgument(0);
                                                p.setIsActive(true);
                                                return p;
                                        });

                        // When
                        ProductResponse response = productService.createProduct(productRequest);

                        // Then
                        assertNotNull(response);
                        assertEquals("Gaming Laptop", response.getProductName());
                        assertEquals("High-performance gaming laptop", response.getDescription());
                        assertEquals(999.0, response.getUnitPrice());
                        assertTrue(response.getIsActive());

                        verify(productRepository, times(2)).findByProductCodeAndIsActiveTrue("LAP001");
                        verify(productRepository).save(productCaptor.capture());
                        Product savedProduct = productCaptor.getValue();
                        assertEquals("Gaming Laptop", savedProduct.getProductName());
                        assertTrue(savedProduct.getIsActive());
                }

                @Test
                void createProduct_fails_whenActiveProductCodeExists() {
                        // Given
                        Product existingActiveProduct = Product.builder()
                                        .productId(1L)
                                        .productCode("LAP001")
                                        .isActive(true)
                                        .build();

                        when(productRepository.findByProductCodeAndIsActiveTrue("LAP001"))
                                        .thenReturn(Optional.of(existingActiveProduct));

                        // When & Then
                        ResourceAlreadyExistsException exception = assertThrows(
                                        ResourceAlreadyExistsException.class,
                                        () -> productService.createProduct(productRequest));
                        assertEquals("Product code or name already exists", exception.getMessage());

                        verify(productRepository, times(1)).findByProductCodeAndIsActiveTrue("LAP001");
                        verify(productRepository, never()).save(any(Product.class));
                }

                @Test
                void createProduct_fails_whenActiveProductNameExists() {
                        // Given
                        Product existingActiveProduct = Product.builder()
                                        .productId(1L)
                                        .productName("Gaming Laptop")
                                        .isActive(true)
                                        .build();

                        when(productRepository.findByProductCodeAndIsActiveTrue("LAP001"))
                                        .thenReturn(Optional.empty());

                        when(productRepository.findByProductNameAndIsActiveTrue("Gaming Laptop"))
                                        .thenReturn(Optional.of(existingActiveProduct));

                        // When & Then
                        ResourceAlreadyExistsException exception = assertThrows(
                                        ResourceAlreadyExistsException.class,
                                        () -> productService.createProduct(productRequest));
                        assertEquals("Product code or name already exists", exception.getMessage());

                        verify(productRepository, times(1)).findByProductCodeAndIsActiveTrue("LAP001");
                        verify(productRepository, times(1)).findByProductNameAndIsActiveTrue("Gaming Laptop");
                        verify(productRepository, never()).save(any(Product.class));
                }

                @Test
                void createProduct_fails_whenCategoryNotFound() {
                        // Given
                        when(productRepository.findByProductCodeAndIsActiveTrue("LAP001"))
                                        .thenReturn(Optional.empty());

                        when(productRepository.findByProductNameAndIsActiveTrue("Gaming Laptop"))
                                        .thenReturn(Optional.empty());

                        when(productRepository.findByProductCode("LAP001"))
                                        .thenReturn(Optional.empty());

                        when(categoryRepository.findById(1L))
                                        .thenReturn(Optional.empty());

                        // When & Then
                        ResourceNotFoundException exception = assertThrows(
                                        ResourceNotFoundException.class,
                                        () -> productService.createProduct(productRequest));
                        assertEquals("Category not found", exception.getMessage());

                        verify(productRepository, times(1)).findByProductCodeAndIsActiveTrue("LAP001");
                        verify(productRepository, never()).save(any(Product.class));
                }
        }

        @Nested
        class GetProductByIdTests {
                @Test
                void getProductById_success() {
                        // Given
                        when(productRepository.findById(1L))
                                        .thenReturn(Optional.of(product));

                        // When
                        ProductResponse response = productService.getProductById(1L);

                        // Then
                        assertNotNull(response);
                        assertEquals(1L, response.getProductId());
                        assertEquals("Gaming Laptop", response.getProductName());
                        assertEquals("Electronics", response.getCategoryName());
                        assertTrue(response.getIsActive());
                }

                @Test
                void getProductById_fails_whenProductNotFound() {
                        // Given
                        when(productRepository.findById(1L))
                                        .thenReturn(Optional.empty());

                        // When & Then
                        ResourceNotFoundException exception = assertThrows(
                                        ResourceNotFoundException.class,
                                        () -> productService.getProductById(1L));
                        assertEquals("Product not found with id: 1", exception.getMessage());
                }

                @Test
                void getProductById_fails_whenProductInactive() {
                        // Given
                        Product inactiveProduct = Product.builder()
                                        .productId(1L)
                                        .isActive(false)
                                        .build();

                        when(productRepository.findById(1L))
                                        .thenReturn(Optional.of(inactiveProduct));

                        // When & Then
                        ResourceNotFoundException exception = assertThrows(
                                        ResourceNotFoundException.class,
                                        () -> productService.getProductById(1L));
                        assertEquals("Product not found with id: 1", exception.getMessage());
                }
        }

        @Nested
        class UpdateProductTests {
                @Test
                void updateProduct_success() {
                        // Given
                        ProductRequest updateRequest = ProductRequest.builder()
                                        .categoryId(1L)
                                        .productCode("LAP002")
                                        .productName("Updated Gaming Laptop")
                                        .description("Updated description")
                                        .unitPrice(1099.0)
                                        .build();

                        when(productRepository.findById(1L))
                                        .thenReturn(Optional.of(product));

                        when(productRepository.findByProductCodeAndIsActiveTrue("LAP002"))
                                        .thenReturn(Optional.empty());

                        when(productRepository.findByProductNameAndIsActiveTrue("Updated Gaming Laptop"))
                                        .thenReturn(Optional.empty());

                        when(categoryRepository.findById(1L))
                                        .thenReturn(Optional.of(category));

                        when(productRepository.save(any(Product.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                        // When
                        ProductResponse response = productService.updateProduct(1L, updateRequest);

                        // Then
                        assertNotNull(response);
                        verify(productRepository).save(productCaptor.capture());
                        Product updatedProduct = productCaptor.getValue();
                        assertEquals("LAP002", updatedProduct.getProductCode());
                        assertEquals("Updated Gaming Laptop", updatedProduct.getProductName());
                        assertEquals("Updated description", updatedProduct.getDescription());
                        assertEquals(1099.0, updatedProduct.getUnitPrice());
                }

                @Test
                void updateProduct_fails_whenProductNotFound() {
                        // Given
                        when(productRepository.findById(1L))
                                        .thenReturn(Optional.empty());

                        // When & Then
                        ResourceNotFoundException exception = assertThrows(
                                        ResourceNotFoundException.class,
                                        () -> productService.updateProduct(1L, productRequest));
                        assertEquals("Product not found", exception.getMessage());
                }

                @Test
                void updateProduct_fails_whenDuplicateProductCode() {
                        // Given
                        Product existingProduct = Product.builder()
                                        .productId(2L)
                                        .productCode("LAP002")
                                        .isActive(true)
                                        .build();

                        ProductRequest updateRequest = ProductRequest.builder()
                                        .productCode("LAP002")
                                        .productName("Gaming Laptop")
                                        .build();

                        when(productRepository.findById(1L))
                                        .thenReturn(Optional.of(product));

                        when(productRepository.findByProductCodeAndIsActiveTrue("LAP002"))
                                        .thenReturn(Optional.of(existingProduct));

                        // When & Then
                        ResourceAlreadyExistsException exception = assertThrows(
                                        ResourceAlreadyExistsException.class,
                                        () -> productService.updateProduct(1L, updateRequest));
                        assertEquals("Product code already exists", exception.getMessage());
                }

                @Test
                void updateProduct_fails_whenProductInactive() {
                        // Given
                        Product inactiveProduct = Product.builder()
                                        .productId(1L)
                                        .isActive(false)
                                        .build();

                        when(productRepository.findById(1L))
                                        .thenReturn(Optional.of(inactiveProduct));

                        // When & Then
                        ResourceNotFoundException exception = assertThrows(
                                        ResourceNotFoundException.class,
                                        () -> productService.updateProduct(1L, productRequest));
                        assertEquals("Product not found with id: 1", exception.getMessage());
                }

                @Test
                void updateProduct_fails_whenDuplicateProductName() {
                        // Given
                        Product existingProduct = Product.builder()
                                        .productId(2L)
                                        .productName("Existing Laptop")
                                        .isActive(true)
                                        .build();

                        ProductRequest updateRequest = ProductRequest.builder()
                                        .productCode("LAP001")
                                        .productName("Existing Laptop")
                                        .build();

                        when(productRepository.findById(1L))
                                        .thenReturn(Optional.of(product));

                        when(productRepository.findByProductNameAndIsActiveTrue("Existing Laptop"))
                                        .thenReturn(Optional.of(existingProduct));

                        // When & Then
                        ResourceAlreadyExistsException exception = assertThrows(
                                        ResourceAlreadyExistsException.class,
                                        () -> productService.updateProduct(1L, updateRequest));
                        assertEquals("Product name already exists", exception.getMessage());
                }

                @Test
                void updateProduct_fails_whenCategoryNotFound() {
                        // Given
                        ProductRequest updateRequest = ProductRequest.builder()
                                        .categoryId(99L)
                                        .productCode("LAP001")
                                        .productName("Gaming Laptop")
                                        .build();

                        when(productRepository.findById(1L))
                                        .thenReturn(Optional.of(product));

                        when(categoryRepository.findById(99L))
                                        .thenReturn(Optional.empty());

                        // When & Then
                        ResourceNotFoundException exception = assertThrows(
                                        ResourceNotFoundException.class,
                                        () -> productService.updateProduct(1L, updateRequest));
                        assertEquals("Category not found", exception.getMessage());
                }
        }

        @Nested
        class DeleteProductTests {
                @Test
                void deleteProduct_success() {
                        // Given
                        when(productRepository.findById(1L))
                                        .thenReturn(Optional.of(product));

                        when(productRepository.save(any(Product.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                        // When
                        productService.deleteProduct(1L);

                        // Then
                        verify(productRepository).save(productCaptor.capture());
                        assertFalse(productCaptor.getValue().getIsActive());
                }
        }

        @Nested
        class ListProductsTests {
                @Test
                void listProducts_noSearch_success() {
                        // Given
                        ProductFilterRequest filter = new ProductFilterRequest();
                        filter.setPage(0);
                        filter.setSize(10);
                        filter.setSortBy("productName");
                        filter.setDirection("ASC");

                        Page<Product> page = new PageImpl<>(List.of(product));
                        when(productRepository.findAllByIsActiveTrue(any(Pageable.class)))
                                        .thenReturn(page);

                        // When
                        Page<ProductResponse> responsePage = productService.listProducts(filter);

                        // Then
                        assertEquals(1, responsePage.getContent().size());
                        assertEquals("Gaming Laptop", responsePage.getContent().get(0).getProductName());
                }

                @Test
                void listProducts_withSearch_success() {
                        // Given
                        ProductFilterRequest filter = new ProductFilterRequest();
                        filter.setPage(0);
                        filter.setSize(10);
                        filter.setSortBy("productName");
                        filter.setDirection("ASC");
                        filter.setSearch("gaming");

                        Page<Product> page = new PageImpl<>(List.of(product));
                        when(productRepository.searchActiveProducts(eq("gaming"), any(Pageable.class)))
                                        .thenReturn(page);

                        // When
                        Page<ProductResponse> responsePage = productService.listProducts(filter);

                        // Then
                        assertEquals(1, responsePage.getContent().size());
                        assertEquals("Gaming Laptop", responsePage.getContent().get(0).getProductName());
                }

                @Test
                void listProducts_withEmptySearch_usesFindAll() {
                        // Given
                        ProductFilterRequest filter = new ProductFilterRequest();
                        filter.setPage(0);
                        filter.setSize(10);
                        filter.setSortBy("productName");
                        filter.setDirection("ASC");
                        filter.setSearch("   "); 

                        Page<Product> page = new PageImpl<>(List.of(product));
                        when(productRepository.findAllByIsActiveTrue(any(Pageable.class)))
                                        .thenReturn(page);

                        // When
                        Page<ProductResponse> responsePage = productService.listProducts(filter);

                        // Then
                        assertEquals(1, responsePage.getContent().size());
                        verify(productRepository).findAllByIsActiveTrue(any(Pageable.class));
                        verify(productRepository, never()).searchActiveProducts(anyString(), any(Pageable.class));
                }
        }
}