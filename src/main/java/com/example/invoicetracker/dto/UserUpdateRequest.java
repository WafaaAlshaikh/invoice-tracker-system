package com.example.invoicetracker.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;
import java.util.Set;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "User update request")
public class UserUpdateRequest {
    @Schema(description = "Username", example = "john_doe_updated")
    private String username;
    
    @Email(message = "Invalid email format")
    @Schema(description = "Email", example = "john.updated@example.com")
    private String email;
    
    @Schema(description = "User roles", example = "[\"USER\", \"AUDITOR\"]")
    private Set<String> roles;
    
    @Schema(description = "Active status", example = "true")
    private Boolean isActive;
}