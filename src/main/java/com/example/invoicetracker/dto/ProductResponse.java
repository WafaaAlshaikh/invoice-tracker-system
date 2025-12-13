package com.example.invoicetracker.dto;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Product response")
public class ProductResponse {
    @Schema(description = "Product ID", example = "1")
    private Long productId;
    
    @Schema(description = "Category ID", example = "1")
    private Long categoryId;
    
    @Schema(description = "Category name", example = "Electronics")
    private String categoryName;
    
    @Schema(description = "Product code", example = "LAPTOP001")
    private String productCode;
    
    @Schema(description = "Product name", example = "Gaming Laptop")
    private String productName;
    
    @Schema(description = "Product description", example = "High-performance gaming laptop")
    private String description;
    
    @Schema(description = "Unit price", example = "999.99")
    private Double unitPrice;
    
    @Schema(description = "Active status", example = "true")
    private Boolean isActive;
}