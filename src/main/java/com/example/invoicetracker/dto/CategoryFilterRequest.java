package com.example.invoicetracker.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Schema(description = "Category filter criteria")
public class CategoryFilterRequest {
    @Schema(description = "Page number (0-based)", example = "0")
    private int page = 0;

    @Schema(description = "Page size", example = "10")
    private int size = 10;

    @Schema(description = "Sort field", example = "categoryName")
    private String sortBy = "categoryName";

    @Schema(description = "Sort direction", example = "ASC")
    private String direction = "ASC";

    @Schema(description = "Search term", example = "electronics")
    private String search;
}
