package com.example.invoicetracker.dto;

import lombok.Data;
import lombok.Builder;
import java.util.Set;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "User response")
public class UserResponse {
    @Schema(description = "User ID", example = "USER001")
    private String userId;
    
    @Schema(description = "Username", example = "john_doe")
    private String username;
    
    @Schema(description = "Email", example = "john@example.com")
    private String email;
    
    @Schema(description = "User roles", example = "[\"USER\", \"AUDITOR\"]")
    private Set<String> roles;
    
    @Schema(description = "Active status", example = "true")
    private Boolean isActive;
    
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private String createdAt;
    
    @Schema(description = "Last update timestamp", example = "2024-01-15T11:30:00")
    private String updatedAt;
}