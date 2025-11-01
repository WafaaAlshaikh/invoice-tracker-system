package com.example.invoicetracker.dto;

import lombok.Data;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Invoice search criteria")
public class InvoiceSearchRequest {
    @Schema(description = "Page number (0-based)", example = "0")
    private int page = 0;
    
    @Schema(description = "Page size", example = "10")
    private int size = 10;
    
    @Schema(description = "Sort field", example = "createdAt")
    private String sortBy = "createdAt";
    
    @Schema(description = "Sort direction", example = "DESC")
    private String direction = "DESC";
    
    @Schema(description = "Search term", example = "january")
    private String search;
    
    @Schema(description = "Start date for filtering", example = "2024-01-01")
    private LocalDate startDate;
    
    @Schema(description = "End date for filtering", example = "2024-01-31")
    private LocalDate endDate;
    
    @Schema(description = "File type filter", example = "PDF")
    private String fileType;
    
    @Schema(description = "Status filter", example = "APPROVED")
    private String status;
}