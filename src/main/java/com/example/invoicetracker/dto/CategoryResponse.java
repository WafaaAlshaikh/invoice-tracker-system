package com.example.invoicetracker.dto;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Category response")
public class CategoryResponse {
    @Schema(description = "Category ID", example = "1")
    private Long categoryId;
    
    @Schema(description = "Category code", example = "ELEC")
    private String categoryCode;
    
    @Schema(description = "Category name", example = "Electronics")
    private String categoryName;
    
    @Schema(description = "Category description", example = "Electronic devices and components")
    private String description;
    
    @Schema(description = "Active status", example = "true")
    private Boolean isActive;
}