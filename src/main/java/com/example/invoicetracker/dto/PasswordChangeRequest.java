package com.example.invoicetracker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Password change request")
public class PasswordChangeRequest {
    @NotBlank(message = "Current password is required")
    @Schema(description = "Current password", example = "oldPassword123", required = true)
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Schema(description = "New password", example = "newPassword123", required = true)
    private String newPassword;
}