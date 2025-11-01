package com.example.invoicetracker.dto;

import com.example.invoicetracker.model.enums.ActionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "Audit log entry response")
public class AuditLogResponse {
    @Schema(description = "Log ID", example = "1")
    private Long logId;
    
    @Schema(description = "User who performed the action", example = "john_doe")
    private String performedBy;
    
    @Schema(description = "Type of action performed")
    private ActionType actionType;
    
    @Schema(description = "Timestamp of the action", example = "2024-01-15T10:30:00")
    private LocalDateTime timestamp;
    
    @Schema(description = "Old values before change")
    private Map<String, Object> oldValues;
    
    @Schema(description = "New values after change")
    private Map<String, Object> newValues;
    
    @Schema(description = "Human-readable description", example = "john_doe updated this invoice: status from PENDING to APPROVED")
    private String description;
}