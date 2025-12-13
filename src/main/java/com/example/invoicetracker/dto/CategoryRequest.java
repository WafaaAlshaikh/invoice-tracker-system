package com.example.invoicetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Category creation/update request")
public class CategoryRequest {
    @NotBlank(message = "Category code is required")
    @Size(max = 50, message = "Category code must be at most 50 characters")
    @Schema(description = "Unique category code", example = "ELEC", required = true)
    private String categoryCode;

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must be at most 100 characters")
    @Schema(description = "Category name", example = "Electronics", required = true)
    private String categoryName;

    @Schema(description = "Category description", example = "Electronic devices and components")
    private String description;
}