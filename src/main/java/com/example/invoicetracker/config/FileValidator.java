package com.example.invoicetracker.config;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Locale;

@Component
public class FileValidator {
    private static final Logger log = LoggerFactory.getLogger(FileValidator.class);
    private final List<String> allowedContentTypes = Arrays.asList("image/jpeg", "image/png", "image/gif", "application/pdf");
    private final List<String> allowedExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".pdf");
    private final long maxFileSize = 10 * 1024 * 1024;

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is missing or empty");
        }
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size must be less than 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG, GIF images and PDF files are allowed");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("File must have an original filename");
        }
        String extension = extractExtension(originalFilename);
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("Invalid file extension. Allowed: " + allowedExtensions);
        }
        log.debug("Validated file {} ({}, {} bytes)", originalFilename, contentType, file.getSize());
    }

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx == -1) return "";
        return filename.substring(idx).toLowerCase(Locale.ROOT);
    }
}