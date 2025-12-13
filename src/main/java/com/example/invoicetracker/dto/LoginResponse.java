package com.example.invoicetracker.dto;

import lombok.*;
import java.util.Set;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Login response")
public class LoginResponse {
    @Schema(description = "JWT token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
    
    @Schema(description = "User information")
    private UserDto user;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "User information in login response")
    public static class UserDto {
        @Schema(description = "User ID", example = "USER001")
        private String userId;
        
        @Schema(description = "Username", example = "john_doe")
        private String username;
        
        @Schema(description = "Email", example = "john@example.com")
        private String email;
        
        @Schema(description = "User roles", example = "[\"USER\"]")
        private Set<String> roles;
    }
}