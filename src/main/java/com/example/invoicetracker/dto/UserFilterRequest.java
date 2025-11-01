package com.example.invoicetracker.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "User filter criteria")
public class UserFilterRequest {
    @Schema(description = "Page number (0-based)", example = "0")
    private int page = 0;
    
    @Schema(description = "Page size", example = "10")
    private int size = 10;
    
    @Schema(description = "Sort field", example = "username")
    private String sortBy = "username";
    
    @Schema(description = "Sort direction", example = "ASC")
    private String direction = "ASC";
    
    @Schema(description = "Search term", example = "john")
    private String search;
}