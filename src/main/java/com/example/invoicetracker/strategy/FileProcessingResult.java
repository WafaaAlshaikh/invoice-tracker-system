package com.example.invoicetracker.strategy;

import com.example.invoicetracker.model.enums.FileType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileProcessingResult {
    private String storedFileName;
    private String originalFileName;
    private Long fileSize;
    private FileType fileType;
    private boolean success;
    private String errorMessage;
}