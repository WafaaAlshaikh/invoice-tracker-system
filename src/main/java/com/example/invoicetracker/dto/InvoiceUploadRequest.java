package com.example.invoicetracker.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Invoice upload request with file")
public class InvoiceUploadRequest {
    @Schema(description = "Invoice file (PDF/Image)", required = true)
    private MultipartFile file;
    
    @Schema(description = "Invoice date in YYYY-MM-DD format", example = "2024-01-15")
    private String invoiceDate;
    
    @Schema(description = "Comma-separated product IDs", example = "1,2,3")
    private String productIds;
    
    @Schema(description = "Comma-separated product quantities", example = "2.0,1.5,3.0")
    private String productQuantities;
    
    @Schema(description = "User ID (for SUPERUSER)", example = "USER001")
    private String userId;
    
    @Schema(description = "File name", example = "january_invoice.pdf")
    private String fileName;
}   