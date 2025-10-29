package com.example.invoicetracker.repository;

import com.example.invoicetracker.model.entity.Category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByCategoryCode(String categoryCode);
    boolean existsByCategoryCode(String categoryCode);
    boolean existsByCategoryName(String categoryName);
    Optional<Category> findByCategoryIdAndIsActiveTrue(Long categoryId);
    Optional<Category> findByCategoryCodeAndIsActiveTrue(String categoryCode);
    Optional<Category> findByCategoryNameAndIsActiveTrue(String categoryName);
    Page<Category> findAllByIsActiveTrue(Pageable pageable);

    @Query("SELECT c FROM Category c " +
       "WHERE c.isActive = true AND " +
       "(LOWER(c.categoryCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
       "OR LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :search, '%')) " +
       "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))")
Page<Category> searchActiveCategories(@Param("search") String search, Pageable pageable);

}
