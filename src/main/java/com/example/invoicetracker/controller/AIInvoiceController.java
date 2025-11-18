package com.example.invoicetracker.controller;

import com.example.invoicetracker.dto.InvoiceExtractionResult;
import com.example.invoicetracker.service.ai.InvoiceExtractorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai/invoices")
@RequiredArgsConstructor
public class AIInvoiceController {

    private final InvoiceExtractorService extractorService;

    @PreAuthorize("hasRole('USER') or hasRole('SUPERUSER')")
    @PostMapping("/extract")
    public ResponseEntity<?> extractInvoiceData(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "File is required"));
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Unsupported file type"));
        }

        InvoiceExtractionResult result = extractorService.extractInvoiceData(file);

        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }
}
