package com.example.invoicetracker.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Types of actions that can be performed")
public enum ActionType {
    @Schema(description = "Create action") CREATE,
    @Schema(description = "Update action") UPDATE,
    @Schema(description = "Delete action") DELETE,
    @Schema(description = "Get action") GET,
    @Schema(description = "View action") VIEW,
    @Schema(description = "Upload action") UPLOAD,
    @Schema(description = "Download action") DOWNLOAD
}