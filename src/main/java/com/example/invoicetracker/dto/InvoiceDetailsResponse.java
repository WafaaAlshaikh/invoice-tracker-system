package com.example.invoicetracker.dto;

import com.example.invoicetracker.model.enums.FileType;
import com.example.invoicetracker.model.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed invoice response")
public class InvoiceDetailsResponse {
    @Schema(description = "Invoice ID", example = "1")
    private Long invoiceId;
    
    @Schema(description = "Username who uploaded the invoice", example = "john_doe")
    private String uploadedByUser;
    
    @Schema(description = "Invoice date", example = "2024-01-15")
    private LocalDate invoiceDate;
    
    @Schema(description = "File type")
    private FileType fileType;
    
    @Schema(description = "File name", example = "january_invoice.pdf")
    private String fileName;
    
    @Schema(description = "Original file name", example = "invoice_001.pdf")
    private String originalFileName;
    
    @Schema(description = "File size in bytes", example = "1024000")
    private Long fileSize;
    
    @Schema(description = "Total amount", example = "1500.75")
    private Double totalAmount;
    
    @Schema(description = "Invoice status")
    private InvoiceStatus status;
    
    @Schema(description = "List of products in the invoice")
    private List<ProductDetail> products;
    
    @Schema(description = "Audit logs for the invoice")
    private List<AuditLogResponse> auditLogs;
    
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last update timestamp", example = "2024-01-15T11:30:00")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Product details in invoice")
    public static class ProductDetail {
        @Schema(description = "Product ID", example = "1")
        private Long productId;
        
        @Schema(description = "Product name", example = "Laptop")
        private String productName;
        
        @Schema(description = "Category name", example = "Electronics")
        private String category;
        
        @Schema(description = "Unit price", example = "999.99")
        private Double unitPrice;
        
        @Schema(description = "Quantity", example = "2.0")
        private Double quantity;
        
        @Schema(description = "Subtotal", example = "1999.98")
        private Double subtotal;
    }
}