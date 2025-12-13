package com.example.invoicetracker.repository;

import com.example.invoicetracker.model.entity.Invoice;
import com.example.invoicetracker.model.entity.User;
import com.example.invoicetracker.model.enums.FileType;
import com.example.invoicetracker.model.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Page<Invoice> findAllByIsActiveTrue(Pageable pageable);
    Page<Invoice> findAllByUploadedByUserAndIsActiveTrue(User user, Pageable pageable);
    long countByUploadedByUserAndIsActiveTrue(User user);
    long countByIsActiveTrue();
    
    @Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.uploadedByUser = :user AND i.isActive = true")
    Double sumTotalAmountByUserAndIsActiveTrue(@Param("user") User user);
    
    @Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.isActive = true")
    Double sumTotalAmountByIsActiveTrue();
    
    @Query("SELECT i FROM Invoice i WHERE i.uploadedByUser = :user AND i.isActive = true AND " +
           "(:search IS NULL OR LOWER(i.fileName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(i.uploadedByUser.username) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:startDate IS NULL OR i.invoiceDate >= :startDate) AND " +
           "(:endDate IS NULL OR i.invoiceDate <= :endDate) AND " +
           "(:fileType IS NULL OR i.fileType = :fileType) AND " +
           "(:status IS NULL OR i.status = :status)")
    Page<Invoice> searchUserInvoices(@Param("user") User user, @Param("search") String search,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
            @Param("fileType") FileType fileType, @Param("status") InvoiceStatus status, Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.isActive = true AND " +
           "(:search IS NULL OR LOWER(i.fileName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(i.uploadedByUser.username) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:startDate IS NULL OR i.invoiceDate >= :startDate) AND " +
           "(:endDate IS NULL OR i.invoiceDate <= :endDate) AND " +
           "(:fileType IS NULL OR i.fileType = :fileType) AND " +
           "(:status IS NULL OR i.status = :status)")
    Page<Invoice> searchAllInvoices(@Param("search") String search, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, @Param("fileType") FileType fileType,
            @Param("status") InvoiceStatus status, Pageable pageable);
}