package com.example.invoicetracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User registration request")
public class SignupRequest {
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



    public static SignupRequestBuilder builder() {
        return new SignupRequestBuilder();
    }

    public static class SignupRequestBuilder {
        private String userId;
        private String username;
        private String password;
        private String email;

        public SignupRequestBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public SignupRequestBuilder username(String username) {
            this.username = username;
            return this;
        }

        public SignupRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        public SignupRequestBuilder email(String email) {
            this.email = email;
            return this;
        }

        public SignupRequest build() {
            SignupRequest signupRequest = new SignupRequest();
            signupRequest.setUserId(userId);
            signupRequest.setUsername(username);
            signupRequest.setPassword(password);
            signupRequest.setEmail(email);
            return signupRequest;
        }
    }

}
