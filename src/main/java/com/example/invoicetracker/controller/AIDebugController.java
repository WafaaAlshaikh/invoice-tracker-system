package com.example.invoicetracker.controller;

import com.example.invoicetracker.service.ai.SimpleGeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai/debug")
@RequiredArgsConstructor
public class AIDebugController {

    private final SimpleGeminiService geminiService;

    @GetMapping("/test-api")
    public ResponseEntity<?> testGeminiAPI() {
        try {
            log.info("Testing Gemini API connection...");
            
            String response = geminiService.testApi();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "response", response,
                    "message", "Gemini API is working correctly!"
            ));
        } catch (Exception e) {
            log.error("Gemini API test failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Gemini API test failed - check API key and configuration"
            ));
        }
    }

    @GetMapping("/config")
    public ResponseEntity<?> checkConfig() {
        return ResponseEntity.ok(Map.of(
                "message", "Configuration check",
                "timestamp", System.currentTimeMillis()
        ));
    }
}