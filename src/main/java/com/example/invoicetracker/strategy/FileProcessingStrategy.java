package com.example.invoicetracker.strategy;

import org.springframework.web.multipart.MultipartFile;

public interface FileProcessingStrategy {
    FileProcessingResult process(MultipartFile file);
    boolean supports(String fileType);
}