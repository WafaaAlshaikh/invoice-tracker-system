package com.example.invoicetracker.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Product filter criteria")
public class ProductFilterRequest {
    @Schema(description = "Page number (0-based)", example = "0")
    private int page = 0;
    
    @Schema(description = "Page size", example = "10")
    private int size = 10;
    
    @Schema(description = "Sort field", example = "productName")
    private String sortBy = "productName";
    
    @Schema(description = "Sort direction", example = "ASC")
    private String direction = "ASC";
    
    @Schema(description = "Search term", example = "laptop")
    private String search;
}