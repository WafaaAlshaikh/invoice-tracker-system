package com.example.invoicetracker.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Data
@Schema(description = "Login request")
public class LoginRequest {
    @NotBlank(message = "Username is required")
    @Schema(description = "Username", example = "john_doe", required = true)
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Password", example = "password123", required = true)
    private String password;
    
    public LoginRequest() {
    }   
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
