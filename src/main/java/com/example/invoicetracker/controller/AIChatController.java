package com.example.invoicetracker.controller;

import com.example.invoicetracker.controller.util.AuthHelper;
import com.example.invoicetracker.service.ai.InvoiceChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Slf4j
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
@Tag(name = "AI Chatbot", description = "AI-powered invoice analytics chatbot")
@SecurityRequirement(name = "bearerAuth")
public class AIChatController {

    private final InvoiceChatbotService chatbotService;
    private final AuthHelper authHelper;

    @PostMapping
    @Operation(
        summary = "Chat with AI assistant",
        description = "Ask questions about your invoices in natural language"
    )
    public ResponseEntity<?> chat(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Query is required"));
        }
        
        Map<String, String> userInfo = authHelper.getUserInfo(authentication);
        String response = chatbotService.chat(
                query,
                userInfo.get("username"),
                userInfo.get("role")
        );
        
        return ResponseEntity.ok(Map.of(
                "query", query,
                "response", response
        ));
    }
    
    @GetMapping("/debug")
    @Operation(
        summary = "Debug - View raw invoice data",
        description = "Shows the exact data that will be sent to the AI (for debugging)"
    )
    public ResponseEntity<?> debugInvoiceData(Authentication authentication) {
        Map<String, String> userInfo = authHelper.getUserInfo(authentication);
        String rawData = chatbotService.getInvoiceDataForDebug(
                userInfo.get("username"),
                userInfo.get("role")
        );
        
        return ResponseEntity.ok(Map.of(
                "username", userInfo.get("username"),
                "role", userInfo.get("role"),
                "invoiceData", rawData
        ));
    }
}