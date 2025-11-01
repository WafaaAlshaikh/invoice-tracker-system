package com.example.invoicetracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Set;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "User creation request")
public class UserRequest {
    @NotBlank(message = "User ID is required")
    @Size(max = 20, message = "User ID must be at most 20 characters")
    @Schema(description = "User ID", example = "USER001", required = true)
    private String userId;

    @NotBlank(message = "Username is required")
    @Size(max = 100, message = "Username must be at most 100 characters")
    @Schema(description = "Username", example = "john_doe", required = true)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "Password", example = "password123", required = true)
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Email", example = "john@example.com", required = true)
    private String email;

    @Schema(description = "User roles", example = "[\"USER\", \"AUDITOR\"]")
    private Set<String> roles;
}