package com.example.invoicetracker.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Supported file types")
public enum FileType {
    @Schema(description = "PDF files") PDF,
    @Schema(description = "Image files (JPEG, PNG, GIF)") IMAGE,
    @Schema(description = "Web form submissions") WEB_FORM
}