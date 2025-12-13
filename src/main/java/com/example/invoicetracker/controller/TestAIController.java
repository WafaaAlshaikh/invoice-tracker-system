package com.example.invoicetracker.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;


@Slf4j
@RestController
@RequestMapping("/ai/test")
@RequiredArgsConstructor
@Tag(name = "AI Test", description = "Test AI endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TestAIController {

    @GetMapping("/hello")
    @PreAuthorize("hasAnyRole('USER', 'SUPERUSER', 'AUDITOR')")
    @Operation(summary = "Test AI endpoint", description = "Simple test to verify AI routes work")
    public ResponseEntity<?> testHello(Authentication authentication) {
        log.info("AI Test endpoint called by: {}", authentication.getName());
        
        return ResponseEntity.ok(Map.of(
            "message", "AI endpoints are working!",
            "user", authentication.getName(),
            "roles", authentication.getAuthorities().toString(),
            "timestamp", System.currentTimeMillis()
        ));
    }

    @PostMapping("/echo")
    @PreAuthorize("hasAnyRole('USER', 'SUPERUSER', 'AUDITOR')")
    @Operation(summary = "Echo test", description = "Echo back the input")
    public ResponseEntity<?> testEcho(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        log.info("AI Echo endpoint called by: {}", authentication.getName());
        
        return ResponseEntity.ok(Map.of(
            "input", request,
            "user", authentication.getName(),
            "echo", "You said: " + request.get("message")
        ));
    }
}