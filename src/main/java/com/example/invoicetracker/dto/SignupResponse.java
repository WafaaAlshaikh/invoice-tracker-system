package com.example.invoicetracker.dto;

import lombok.*;
import java.util.Set;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "User registration response")
public class SignupResponse {
    @Schema(description = "Response message", example = "User registered successfully")
    private String message;
    
    @Schema(description = "User information")
    private UserDto user;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "User information in registration response")
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