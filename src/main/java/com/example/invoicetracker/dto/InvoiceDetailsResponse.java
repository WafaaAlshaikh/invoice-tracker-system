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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDetailsResponse {
    private Long invoiceId;
    private String uploadedByUser;
    private LocalDate invoiceDate;
    private FileType fileType;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private Double totalAmount;
    private InvoiceStatus status;
    private List<ProductDetail> products;
    private List<AuditLogResponse> auditLogs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDetail {
        private Long productId;
        private String productName;
        private String category;
        private Double unitPrice;
        private Double quantity;
        private Double subtotal;
    }
}