package com.example.invoicetracker.repository;

import com.example.invoicetracker.model.entity.Product;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByProductCode(String productCode);

    Optional<Product> findByProductCodeAndIsActiveTrue(String productCode);

    Optional<Product> findByProductCode(String productCode);

    Optional<Product> findByProductNameAndIsActiveTrue(String productName);

    @Query("SELECT p FROM Product p WHERE p.productId IN :ids AND p.isActive = true")
    List<Product> findAllByIdAndIsActiveTrue(@Param("ids") Set<Long> ids);

    Page<Product> findAllByIsActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Product p " +
            "WHERE p.isActive = true AND " +
            "(LOWER(p.productCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.category.categoryName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchActiveProducts(@Param("search") String search, Pageable pageable);
}
