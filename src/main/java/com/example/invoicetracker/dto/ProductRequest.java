package com.example.invoicetracker.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Product creation/update request")
public class ProductRequest {
    @NotNull(message = "Category ID is required")
    @Schema(description = "Category ID", example = "1", required = true)
    private Long categoryId;

    @NotBlank(message = "Product code is required")
    @Schema(description = "Unique product code", example = "LAPTOP001", required = true)
    private String productCode;

    @NotBlank(message = "Product name is required")
    @Schema(description = "Product name", example = "Gaming Laptop", required = true)
    private String productName;

    @Schema(description = "Product description", example = "High-performance gaming laptop")
    private String description;

    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be positive")
    @Schema(description = "Unit price", example = "999.99", required = true)
    private Double unitPrice;
}