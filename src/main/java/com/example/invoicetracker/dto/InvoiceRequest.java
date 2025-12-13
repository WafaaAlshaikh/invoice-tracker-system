package com.example.invoicetracker.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.example.invoicetracker.model.enums.FileType;
import com.example.invoicetracker.model.enums.InvoiceStatus;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
    
@Data
@Schema(description = "Invoice creation/update request")
public class InvoiceRequest {
    
    @NotNull(message = "Invoice date is required")
    @Schema(description = "Invoice date", example = "2024-01-15", required = true)
    private LocalDate invoiceDate;
    
    @Schema(description = "File name", example = "january_invoice.pdf")
    private String fileName;
    
    @Schema(description = "File type (OPTIONAL - will be determined automatically)")
    private FileType fileType;
    
    @Schema(description = "List of product IDs", example = "[1, 2, 3]")
    private List<Long> productIds;
    
    @Schema(description = "Map of product IDs to quantities", example = "{\"1\": 2.0, \"2\": 1.5}")
    private Map<Long, Double> productQuantities;
    
    @Schema(description = "User ID (for SUPERUSER creating invoices for other users)")
    private String userId;
    
    @Schema(description = "Uploaded file (for multipart requests)")
    private MultipartFile file;
    
    @Schema(description = "Invoice status (OPTIONAL)")
    private InvoiceStatus status;

    @Schema(description = "Total amount (OPTIONAL - can be calculated from products or extracted from file)")
    private Double totalAmount;
    
    @Schema(description = "Vendor name (OPTIONAL)")
    private String vendor;
}