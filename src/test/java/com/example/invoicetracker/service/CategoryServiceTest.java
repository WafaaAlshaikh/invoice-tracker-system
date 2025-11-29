package com.example.invoicetracker.service;

import com.example.invoicetracker.dto.CategoryFilterRequest;
import com.example.invoicetracker.dto.CategoryRequest;
import com.example.invoicetracker.dto.CategoryResponse;
import com.example.invoicetracker.exception.ResourceAlreadyExistsException;
import com.example.invoicetracker.exception.ResourceNotFoundException;
import com.example.invoicetracker.model.entity.Category;
import com.example.invoicetracker.repository.CategoryRepository;
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
class CategoryServiceTest {

        @Mock
        private CategoryRepository categoryRepository;

        @InjectMocks
        private CategoryService categoryService;

        @Captor
        private ArgumentCaptor<Category> categoryCaptor;

        private CategoryRequest categoryRequest;
        private Category category;

        @BeforeEach
        void setup() {
                categoryRequest = CategoryRequest.builder()
                                .categoryCode("ELEC")
                                .categoryName("Electronics")
                                .description("Electronic devices and components")
                                .build();

                category = Category.builder()
                                .categoryId(1L)
                                .categoryCode("ELEC")
                                .categoryName("Electronics")
                                .description("Electronic devices and components")
                                .isActive(true)
                                .build();
        }

        @Nested
        class CreateCategoryTests {
                @Test
                void createCategory_success_newCategory() {
                        // Given
                        when(categoryRepository.findByCategoryCodeAndIsActiveTrue("ELEC"))
                                        .thenReturn(Optional.empty())
                                        .thenReturn(Optional.of(category));

                        when(categoryRepository.findByCategoryNameAndIsActiveTrue("Electronics"))
                                        .thenReturn(Optional.empty());

                        when(categoryRepository.findByCategoryCode("ELEC"))
                                        .thenReturn(Optional.empty());

                        when(categoryRepository.save(any(Category.class)))
                                        .thenAnswer(invocation -> {
                                                Category cat = invocation.getArgument(0);
                                                cat.setCategoryId(1L);
                                                return cat;
                                        });

                        // When
                        CategoryResponse response = categoryService.createCategory(categoryRequest);

                        // Then
                        assertNotNull(response);
                        assertEquals("ELEC", response.getCategoryCode());
                        assertEquals("Electronics", response.getCategoryName());
                        assertEquals("Electronic devices and components", response.getDescription());
                        assertTrue(response.getIsActive());

                        verify(categoryRepository, times(2)).findByCategoryCodeAndIsActiveTrue("ELEC");
                        verify(categoryRepository).findByCategoryNameAndIsActiveTrue("Electronics");
                        verify(categoryRepository).findByCategoryCode("ELEC");
                        verify(categoryRepository).save(any(Category.class));
                }

                @Test
                void createCategory_success_reactivateInactiveCategory() {
                        // Given
                        Category inactiveCategory = Category.builder()
                                        .categoryId(2L)
                                        .categoryCode("ELEC")
                                        .categoryName("Old Electronics")
                                        .description("Old description")
                                        .isActive(false)
                                        .build();

                        when(categoryRepository.findByCategoryCodeAndIsActiveTrue("ELEC"))
                                        .thenReturn(Optional.empty()) 
                                        .thenReturn(Optional.of(inactiveCategory)); 

                        when(categoryRepository.findByCategoryNameAndIsActiveTrue("Electronics"))
                                        .thenReturn(Optional.empty());

                        when(categoryRepository.findByCategoryCode("ELEC"))
                                        .thenReturn(Optional.of(inactiveCategory));

                        when(categoryRepository.save(any(Category.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                        // When
                        CategoryResponse response = categoryService.createCategory(categoryRequest);

                        // Then
                        assertNotNull(response);
                        assertEquals("Electronics", response.getCategoryName());
                        assertEquals("Electronic devices and components", response.getDescription());
                        assertTrue(response.getIsActive());

                        verify(categoryRepository, times(2)).findByCategoryCodeAndIsActiveTrue("ELEC");
                        verify(categoryRepository).save(categoryCaptor.capture());
                        Category savedCategory = categoryCaptor.getValue();
                        assertEquals("Electronics", savedCategory.getCategoryName());
                        assertTrue(savedCategory.getIsActive());
                }

                @Test
                void createCategory_fails_whenActiveCategoryCodeExists() {
                        // Given
                        Category existingActiveCategory = Category.builder()
                                        .categoryId(1L)
                                        .categoryCode("ELEC")
                                        .isActive(true)
                                        .build();

                        when(categoryRepository.findByCategoryCodeAndIsActiveTrue("ELEC"))
                                        .thenReturn(Optional.of(existingActiveCategory));

                        // When & Then
                        ResourceAlreadyExistsException exception = assertThrows(
                                        ResourceAlreadyExistsException.class,
                                        () -> categoryService.createCategory(categoryRequest));
                        assertEquals("Category code or name already exists", exception.getMessage());

                        verify(categoryRepository, times(1)).findByCategoryCodeAndIsActiveTrue("ELEC");
                        verify(categoryRepository, never()).save(any(Category.class));
                }

                @Test
                void createCategory_fails_whenActiveCategoryNameExists() {
                        // Given
                        Category existingActiveCategory = Category.builder()
                                        .categoryId(1L)
                                        .categoryName("Electronics")
                                        .isActive(true)
                                        .build();

                        when(categoryRepository.findByCategoryCodeAndIsActiveTrue("ELEC"))
                                        .thenReturn(Optional.empty());

                        when(categoryRepository.findByCategoryNameAndIsActiveTrue("Electronics"))
                                        .thenReturn(Optional.of(existingActiveCategory));

                        // When & Then
                        ResourceAlreadyExistsException exception = assertThrows(
                                        ResourceAlreadyExistsException.class,
                                        () -> categoryService.createCategory(categoryRequest));
                        assertEquals("Category code or name already exists", exception.getMessage());

                        verify(categoryRepository, times(1)).findByCategoryCodeAndIsActiveTrue("ELEC");
                        verify(categoryRepository, times(1)).findByCategoryNameAndIsActiveTrue("Electronics");
                        verify(categoryRepository, never()).save(any(Category.class));
                }
        }

        @Nested
        class GetCategoryByIdTests {
                @Test
                void getCategoryById_success() {
                        // Given
                        when(categoryRepository.findByCategoryIdAndIsActiveTrue(1L))
                                        .thenReturn(Optional.of(category));

                        // When
                        CategoryResponse response = categoryService.getCategoryById(1L);

                        // Then
                        assertNotNull(response);
                        assertEquals(1L, response.getCategoryId());
                        assertEquals("ELEC", response.getCategoryCode());
                        assertEquals("Electronics", response.getCategoryName());
                        assertTrue(response.getIsActive());
                }

                @Test
                void getCategoryById_fails_whenCategoryNotFound() {
                        // Given
                        when(categoryRepository.findByCategoryIdAndIsActiveTrue(1L))
                                        .thenReturn(Optional.empty());

                        // When & Then
                        ResourceNotFoundException exception = assertThrows(
                                        ResourceNotFoundException.class,
                                        () -> categoryService.getCategoryById(1L));
                        assertEquals("Category not found with id: 1", exception.getMessage());
                }

                @Test
                void getCategoryById_fails_whenCategoryInactive() {
                        // Given
                        Category inactiveCategory = Category.builder()
                                        .categoryId(1L)
                                        .isActive(false)
                                        .build();

                        when(categoryRepository.findByCategoryIdAndIsActiveTrue(1L))
                                        .thenReturn(Optional.empty());

                        // When & Then
                        ResourceNotFoundException exception = assertThrows(
                                        ResourceNotFoundException.class,
                                        () -> categoryService.getCategoryById(1L));
                        assertEquals("Category not found with id: 1", exception.getMessage());
                }
        }

        @Nested
        class UpdateCategoryTests {
                @Test
                void updateCategory_success() {
                        // Given
                        CategoryRequest updateRequest = CategoryRequest.builder()
                                        .categoryCode("ELEC")
                                        .categoryName("Updated Electronics")
                                        .description("Updated description")
                                        .build();

                        when(categoryRepository.findById(1L))
                                        .thenReturn(Optional.of(category));
                        when(categoryRepository.save(any(Category.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                        // When
                        CategoryResponse response = categoryService.updateCategory(1L, updateRequest);

                        // Then
                        assertNotNull(response);
                        verify(categoryRepository).save(categoryCaptor.capture());
                        Category updatedCategory = categoryCaptor.getValue();
                        assertEquals("Updated Electronics", updatedCategory.getCategoryName());
                        assertEquals("Updated description", updatedCategory.getDescription());
                }

                @Test
                void updateCategory_success_sameCodeAndName() {
                        // Given
                        CategoryRequest updateRequest = CategoryRequest.builder()
                                        .categoryCode("ELEC") 
                                        .categoryName("Electronics") 
                                        .description("Updated description")
                                        .build();

                        when(categoryRepository.findById(1L))
                                        .thenReturn(Optional.of(category));

                        when(categoryRepository.save(any(Category.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                        // When
                        CategoryResponse response = categoryService.updateCategory(1L, updateRequest);

                        // Then
                        assertNotNull(response);
                        verify(categoryRepository, never()).findByCategoryCodeAndIsActiveTrue(anyString());
                        verify(categoryRepository, never()).findByCategoryNameAndIsActiveTrue(anyString());
                        verify(categoryRepository).save(categoryCaptor.capture());
                        Category updatedCategory = categoryCaptor.getValue();
                        assertEquals("Updated description", updatedCategory.getDescription());
                }

                @Test
                void updateCategory_success_changedCode() {
                        // Given
                        CategoryRequest updateRequest = CategoryRequest.builder()
                                        .categoryCode("NEWCODE") 
                                        .categoryName("Electronics") 
                                        .description("Updated description")
                                        .build();

                        when(categoryRepository.findById(1L))
                                        .thenReturn(Optional.of(category));

                        when(categoryRepository.findByCategoryCodeAndIsActiveTrue("NEWCODE"))
                                        .thenReturn(Optional.empty());

                        when(categoryRepository.save(any(Category.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                        // When
                        CategoryResponse response = categoryService.updateCategory(1L, updateRequest);

                        // Then
                        assertNotNull(response);
                        verify(categoryRepository).findByCategoryCodeAndIsActiveTrue("NEWCODE");
                        verify(categoryRepository, never()).findByCategoryNameAndIsActiveTrue(anyString());
                        verify(categoryRepository).save(categoryCaptor.capture());
                        Category updatedCategory = categoryCaptor.getValue();
                        assertEquals("NEWCODE", updatedCategory.getCategoryCode());
                }

                @Test
                void updateCategory_success_changedName() {
                        // Given
                        CategoryRequest updateRequest = CategoryRequest.builder()
                                        .categoryCode("ELEC") 
                                        .categoryName("New Electronics") 
                                        .description("Updated description")
                                        .build();

                        when(categoryRepository.findById(1L))
                                        .thenReturn(Optional.of(category));

                        when(categoryRepository.findByCategoryNameAndIsActiveTrue("New Electronics"))
                                        .thenReturn(Optional.empty());

                        when(categoryRepository.save(any(Category.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                        // When
                        CategoryResponse response = categoryService.updateCategory(1L, updateRequest);

                        // Then
                        assertNotNull(response);
                        verify(categoryRepository, never()).findByCategoryCodeAndIsActiveTrue(anyString());
                        verify(categoryRepository).findByCategoryNameAndIsActiveTrue("New Electronics");
                        verify(categoryRepository).save(categoryCaptor.capture());
                        Category updatedCategory = categoryCaptor.getValue();
                        assertEquals("New Electronics", updatedCategory.getCategoryName());
                }

                @Test
                void updateCategory_fails_whenCategoryNotFound() {
                        // Given
                        when(categoryRepository.findById(1L))
                                        .thenReturn(Optional.empty());

                        // When & Then
                        ResourceNotFoundException exception = assertThrows(
                                        ResourceNotFoundException.class,
                                        () -> categoryService.updateCategory(1L, categoryRequest));
                        assertEquals("Category not found with id: 1", exception.getMessage());
                }

                @Test
                void updateCategory_fails_whenCategoryInactive() {
                        // Given
                        Category inactiveCategory = Category.builder()
                                        .categoryId(1L)
                                        .isActive(false)
                                        .build();

                        when(categoryRepository.findById(1L))
                                        .thenReturn(Optional.of(inactiveCategory));

                        // When & Then
                        ResourceNotFoundException exception = assertThrows(
                                        ResourceNotFoundException.class,
                                        () -> categoryService.updateCategory(1L, categoryRequest));
                        assertEquals("Category not found with id: 1", exception.getMessage());
                }

                @Test
                void updateCategory_fails_whenDuplicateCategoryCode() {
                        // Given
                        Category existingCategory = Category.builder()
                                        .categoryId(2L)
                                        .categoryCode("NEWCODE")
                                        .isActive(true)
                                        .build();

                        CategoryRequest updateRequest = CategoryRequest.builder()
                                        .categoryCode("NEWCODE")
                                        .categoryName("Electronics")
                                        .build();

                        when(categoryRepository.findById(1L))
                                        .thenReturn(Optional.of(category));

                        when(categoryRepository.findByCategoryCodeAndIsActiveTrue("NEWCODE"))
                                        .thenReturn(Optional.of(existingCategory));

                        // When & Then
                        ResourceAlreadyExistsException exception = assertThrows(
                                        ResourceAlreadyExistsException.class,
                                        () -> categoryService.updateCategory(1L, updateRequest));
                        assertEquals("Category code already exists", exception.getMessage());
                }

                @Test
                void updateCategory_fails_whenDuplicateCategoryName() {
                        // Given
                        Category existingCategory = Category.builder()
                                        .categoryId(2L)
                                        .categoryName("New Electronics")
                                        .isActive(true)
                                        .build();

                        CategoryRequest updateRequest = CategoryRequest.builder()
                                        .categoryCode("ELEC")
                                        .categoryName("New Electronics") 
                                        .build();

                        when(categoryRepository.findById(1L))
                                        .thenReturn(Optional.of(category));

                        

                        when(categoryRepository.findByCategoryNameAndIsActiveTrue("New Electronics"))
                                        .thenReturn(Optional.of(existingCategory));

                        // When & Then
                        ResourceAlreadyExistsException exception = assertThrows(
                                        ResourceAlreadyExistsException.class,
                                        () -> categoryService.updateCategory(1L, updateRequest));
                        assertEquals("Category name already exists", exception.getMessage());
                }
        }

        @Nested
        class DeleteCategoryTests {
                @Test
                void deleteCategory_success() {
                        // Given
                        when(categoryRepository.findById(1L))
                                        .thenReturn(Optional.of(category));

                        when(categoryRepository.save(any(Category.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                        // When
                        categoryService.deleteCategory(1L);

                        // Then
                        verify(categoryRepository).save(categoryCaptor.capture());
                        assertFalse(categoryCaptor.getValue().getIsActive());
                }

                @Test
                void deleteCategory_fails_whenCategoryNotFound() {
                        // Given
                        when(categoryRepository.findById(1L))
                                        .thenReturn(Optional.empty());

                        // When & Then
                        ResourceNotFoundException exception = assertThrows(
                                        ResourceNotFoundException.class,
                                        () -> categoryService.deleteCategory(1L));
                        assertEquals("Category not found with id: 1", exception.getMessage());
                        verify(categoryRepository, never()).save(any(Category.class));
                }
        }

        @Nested
        class ListCategoriesTests {
                @Test
                void listCategories_noSearch_success() {
                        // Given
                        CategoryFilterRequest filter = new CategoryFilterRequest();
                        filter.setPage(0);
                        filter.setSize(10);
                        filter.setSortBy("categoryName");
                        filter.setDirection("ASC");

                        Page<Category> page = new PageImpl<>(List.of(category));
                        when(categoryRepository.findAllByIsActiveTrue(any(Pageable.class)))
                                        .thenReturn(page);

                        // When
                        Page<CategoryResponse> responsePage = categoryService.listCategories(filter);

                        // Then
                        assertEquals(1, responsePage.getContent().size());
                        assertEquals("ELEC", responsePage.getContent().get(0).getCategoryCode());
                        verify(categoryRepository).findAllByIsActiveTrue(any(Pageable.class));
                        verify(categoryRepository, never()).searchActiveCategories(anyString(), any(Pageable.class));
                }

                @Test
                void listCategories_withSearch_success() {
                        // Given
                        CategoryFilterRequest filter = new CategoryFilterRequest();
                        filter.setPage(0);
                        filter.setSize(10);
                        filter.setSortBy("categoryName");
                        filter.setDirection("ASC");
                        filter.setSearch("Electronics");

                        Page<Category> page = new PageImpl<>(List.of(category));
                        when(categoryRepository.searchActiveCategories(eq("Electronics"), any(Pageable.class)))
                                        .thenReturn(page);

                        // When
                        Page<CategoryResponse> responsePage = categoryService.listCategories(filter);

                        // Then
                        assertEquals(1, responsePage.getContent().size());
                        assertEquals("ELEC", responsePage.getContent().get(0).getCategoryCode());
                        verify(categoryRepository).searchActiveCategories(eq("Electronics"), any(Pageable.class));
                        verify(categoryRepository, never()).findAllByIsActiveTrue(any(Pageable.class));
                }

                @Test
                void listCategories_emptySearch_success() {
                        // Given
                        CategoryFilterRequest filter = new CategoryFilterRequest();
                        filter.setPage(0);
                        filter.setSize(10);
                        filter.setSortBy("categoryName");
                        filter.setDirection("ASC");
                        filter.setSearch("   "); // empty search

                        Page<Category> page = new PageImpl<>(List.of(category));
                        when(categoryRepository.findAllByIsActiveTrue(any(Pageable.class)))
                                        .thenReturn(page);

                        // When
                        Page<CategoryResponse> responsePage = categoryService.listCategories(filter);

                        // Then
                        assertEquals(1, responsePage.getContent().size());
                        verify(categoryRepository).findAllByIsActiveTrue(any(Pageable.class));
                }

                @Test
                void listCategories_emptyResult() {
                        // Given
                        CategoryFilterRequest filter = new CategoryFilterRequest();
                        filter.setPage(0);
                        filter.setSize(10);
                        filter.setSortBy("categoryName");
                        filter.setDirection("ASC");

                        Page<Category> emptyPage = new PageImpl<>(List.of());
                        when(categoryRepository.findAllByIsActiveTrue(any(Pageable.class)))
                                        .thenReturn(emptyPage);

                        // When
                        Page<CategoryResponse> responsePage = categoryService.listCategories(filter);

                        // Then
                        assertTrue(responsePage.getContent().isEmpty());
                        assertEquals(0, responsePage.getTotalElements());
                }

                @Test
                void listCategories_withDifferentPagination() {
                        // Given
                        CategoryFilterRequest filter = new CategoryFilterRequest();
                        filter.setPage(1);
                        filter.setSize(5);
                        filter.setSortBy("categoryCode");
                        filter.setDirection("DESC");

                        Page<Category> page = new PageImpl<>(List.of(category));
                        when(categoryRepository.findAllByIsActiveTrue(any(Pageable.class)))
                                        .thenReturn(page);

                        // When
                        Page<CategoryResponse> responsePage = categoryService.listCategories(filter);

                        // Then
                        assertEquals(1, responsePage.getContent().size());
                        verify(categoryRepository).findAllByIsActiveTrue(any(Pageable.class));
                }
        }
}