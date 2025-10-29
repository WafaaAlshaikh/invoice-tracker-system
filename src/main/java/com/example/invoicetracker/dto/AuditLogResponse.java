package com.example.invoicetracker.dto;

import com.example.invoicetracker.model.enums.ActionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class AuditLogResponse {
    private Long logId;
    private String performedBy;
    private ActionType actionType;
    private LocalDateTime timestamp;
    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    private String description;
}