package com.example.invoicetracker.service.ai;

import com.example.invoicetracker.dto.InvoiceExtractionResult;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceExtractorService {

    private final GeminiService geminiService;
    private final Gson gson;

    // ==================== MAIN EXTRACTION METHOD ====================
    public InvoiceExtractionResult extractInvoiceData(MultipartFile file) {
        try {
            log.info("Starting AI invoice extraction for file: {} ({} bytes)",
                    file.getOriginalFilename(), file.getSize());

            if (file.isEmpty()) return buildErrorResult("File is empty");

            if (file.getSize() > 4 * 1024 * 1024)
                return buildErrorResult("File too large. Maximum 4MB allowed.");

            String prompt = buildExtractionPrompt();

            String aiResponse = geminiService.analyzeImage(file, prompt);

            return parseAiResponse(aiResponse);

        } catch (Exception e) {
            log.error("Error extracting invoice data: {}", e.getMessage(), e);
            return buildErrorResult("Extraction failed: " + e.getMessage());
        }
    }

    // ==================== PROMPT BUILDER ====================
    private String buildExtractionPrompt() {
        return """
            You are an expert invoice data extraction system. Extract ALL invoice information from this document.
            
            CRITICAL INSTRUCTIONS:
            1. Return ONLY valid JSON - no markdown, no explanations, no extra text
            2. Extract dates in YYYY-MM-DD format (ISO 8601)
            3. Extract all monetary amounts as numbers (no currency symbols)
            4. Handle both Arabic and English text
            5. If multiple date formats exist, prefer the invoice date over created/printed dates
            6. Extract ALL items/products listed in the invoice
            
            REQUIRED JSON STRUCTURE:
            {
              "invoiceDate": "2024-01-15",
              "totalAmount": 1500.75,
              "vendor": "Company Name",
              "items": [
                {
                  "name": "Product Name 1",
                  "quantity": 2.0,
                  "unitPrice": 500.0,
                  "subtotal": 1000.0
                },
                {
                  "name": "Product Name 2", 
                  "quantity": 1.5,
                  "unitPrice": 200.0,
                  "subtotal": 300.0
                }
              ]
            }
            
            FIELD EXTRACTION RULES:
            - invoiceDate: Look for "Invoice Date", "Date", "التاريخ", "تاريخ الفاتورة" (format: YYYY-MM-DD)
            - totalAmount: Look for "Total", "Grand Total", "المجموع", "الإجمالي" (number only, no currency)
            - vendor: Look for "Vendor", "Supplier", "Company Name", "البائع", "المورد"
            - items: Extract all line items with their details
              * For each item, if subtotal is missing but quantity and unitPrice exist, calculate: subtotal = quantity × unitPrice
            
            EDGE CASES:
            - If a field is not found or unclear, use null
            - If date format is ambiguous (DD/MM vs MM/DD), prefer DD/MM/YYYY
            - If multiple totals exist (subtotal, tax, grand total), use the grand total
            - Remove currency symbols from amounts (e.g., "$1,500.00" → 1500.00)
            - Convert Arabic numerals to Western numerals if needed
            
            VALIDATION:
            - Ensure all JSON brackets and braces are closed
            - Use double quotes for all string values
            - Numbers should be unquoted
            - Array of items must be valid even if empty: []
            
            Return ONLY the JSON object, nothing else.
            """;
    }

    // ==================== RESPONSE PARSER ====================
    private InvoiceExtractionResult parseAiResponse(String aiResponse) {
        try {
            if (aiResponse == null || aiResponse.isBlank()) {
                return buildErrorResult("AI response is empty");
            }

            log.info("🔧 Raw AI Response: {}", aiResponse);

            String cleaned = aiResponse
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .replaceAll("^\\s*JSON:\\s*", "") 
                    .replaceAll("\\n", " ") 
                    .trim();

            log.info("🔧 Cleaned response: {}", cleaned);

            String fixedJson = fixJsonString(cleaned);

            JsonReader reader = new JsonReader(new StringReader(fixedJson));
            reader.setLenient(true); 

            JsonElement element = gson.fromJson(reader, JsonElement.class);

            if (!element.isJsonObject()) {
                log.warn("⚠️ Response is not a JSON object: {}", cleaned);
                return buildErrorResult("AI response is not valid JSON object");
            }

            JsonObject json = element.getAsJsonObject();
            log.info("✅ Successfully parsed JSON object");

            LocalDate invoiceDate = extractInvoiceDate(json);
            Double totalAmount = extractTotalAmount(json);
            String vendor = extractVendor(json);
            List<InvoiceExtractionResult.ExtractedItem> items = extractItems(json);

            // Calculate total from items if not provided
            if (totalAmount == null && items != null && !items.isEmpty()) {
                totalAmount = calculateTotalFromItems(items);
                if (totalAmount != null) {
                    log.info("📊 Total amount not found in extraction - Calculated from items: ${}", totalAmount);
                } else {
                    log.warn("⚠️ Total amount missing and cannot be calculated from items (missing quantity/price data)");
                }
            } else if (totalAmount == null) {
                log.warn("⚠️ No total amount found and no items to calculate from");
            }

            // Final validation
            boolean hasUsefulData = totalAmount != null || 
                                   invoiceDate != null || 
                                   vendor != null || 
                                   (items != null && !items.isEmpty());
            
            if (!hasUsefulData) {
                log.error("❌ Extraction produced no useful data");
                return buildErrorResult("AI could not extract any useful invoice data from the file");
            }

            return InvoiceExtractionResult.builder()
                    .success(true)
                    .invoiceDate(invoiceDate)
                    .totalAmount(totalAmount)
                    .vendor(vendor)
                    .extractedItems(items)
                    .rawResponse(aiResponse)
                    
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to parse AI response: {}", e.getMessage());
            log.debug("🔧 Response that caused error: {}", aiResponse);
            return buildErrorResult("Failed to parse AI response: " + e.getMessage());
        }
    }

    // ==================== JSON FIXER ====================
    
    private String fixJsonString(String json) {
        if (json == null) return "{}";

        String fixed = json
                .replaceAll("([{,}\\s])([a-zA-Z_][a-zA-Z0-9_]*):\\s*([^\"][^,}\\]]*?)([,}\\])])", "$1\"$2\": \"$3\"$4")
                .replaceAll("([{,}\\s])([a-zA-Z_][a-zA-Z0-9_]*):\\s*([^\"'][^,}\\]]*?)([,}\\])])", "$1\"$2\": \"$3\"$4");

        try {
            gson.fromJson(fixed, JsonObject.class);
            log.info("✅ JSON fixed successfully");
            return fixed;
        } catch (Exception e) {
            log.warn("⚠️ JSON fixing failed, using original: {}", e.getMessage());
            return json;
        }
    }

    // ==================== DATA EXTRACTORS ====================
    
    /**
     * Calculates total amount from invoice items.
     * Uses subtotal if available, otherwise calculates from quantity * unitPrice.
     */
    private Double calculateTotalFromItems(List<InvoiceExtractionResult.ExtractedItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        
        double total = 0.0;
        int validItems = 0;
        
        for (InvoiceExtractionResult.ExtractedItem item : items) {
            Double itemAmount = null;
            
            // Priority 1: Use subtotal if available
            if (item.getSubtotal() != null) {
                itemAmount = item.getSubtotal();
            }
            // Priority 2: Calculate from quantity * unitPrice
            else if (item.getQuantity() != null && item.getUnitPrice() != null) {
                itemAmount = item.getQuantity() * item.getUnitPrice();
            }
            
            if (itemAmount != null) {
                total += itemAmount;
                validItems++;
                log.debug("  Item '{}': ${}", item.getName(), itemAmount);
            }
        }
        
        if (validItems == 0) {
            log.warn("⚠️ No valid items found to calculate total");
            return null;
        }
        
        log.info("✅ Calculated total from {} items: ${}", validItems, total);
        return total;
    }
    
    
    private LocalDate extractInvoiceDate(JsonObject json) {
        if (!json.has("invoiceDate") || json.get("invoiceDate").isJsonNull()) {
            return null;
        }

        try {
            String dateStr = json.get("invoiceDate").getAsString();
            if (dateStr == null || dateStr.trim().isEmpty()) {
                return null;
            }
            return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            log.warn("⚠️ Invalid invoice date format: {}", json.get("invoiceDate").getAsString());
            return null;
        }
    }

    
    
    private Double extractTotalAmount(JsonObject json) {
        if (!json.has("totalAmount") || json.get("totalAmount").isJsonNull()) {
            return null;
        }

        try {
            return json.get("totalAmount").getAsDouble();
        } catch (Exception e) {
            log.warn("⚠️ Invalid total amount: {}", json.get("totalAmount"));
            return null;
        }
    }

    
    
    private String extractVendor(JsonObject json) {
        if (!json.has("vendor") || json.get("vendor").isJsonNull()) {
            return null;
        }

        try {
            String vendor = json.get("vendor").getAsString();
            return (vendor != null && !vendor.trim().isEmpty()) ? vendor.trim() : null;
        } catch (Exception e) {
            log.warn("⚠️ Invalid vendor: {}", json.get("vendor"));
            return null;
        }
    }

    
    
    private List<InvoiceExtractionResult.ExtractedItem> extractItems(JsonObject json) {
        List<InvoiceExtractionResult.ExtractedItem> items = new ArrayList<>();

        if (!json.has("items") || !json.get("items").isJsonArray()) {
            return items;
        }

        json.getAsJsonArray("items").forEach(elem -> {
            try {
                if (elem.isJsonObject()) {
                    JsonObject itemObj = elem.getAsJsonObject();

                    String name = null;
                    if (itemObj.has("name") && !itemObj.get("name").isJsonNull()) {
                        name = itemObj.get("name").getAsString();
                    }

                    Double quantity = null;
                    if (itemObj.has("quantity") && !itemObj.get("quantity").isJsonNull()) {
                        quantity = itemObj.get("quantity").getAsDouble();
                    }

                    Double unitPrice = null;
                    if (itemObj.has("unitPrice") && !itemObj.get("unitPrice").isJsonNull()) {
                        unitPrice = itemObj.get("unitPrice").getAsDouble();
                    }

                    Double subtotal = null;
                    if (itemObj.has("subtotal") && !itemObj.get("subtotal").isJsonNull()) {
                        subtotal = itemObj.get("subtotal").getAsDouble();
                    }

                    if (name != null || quantity != null || unitPrice != null) {
                        items.add(InvoiceExtractionResult.ExtractedItem.builder()
                                .name(name)
                                .quantity(quantity)
                                .unitPrice(unitPrice)
                                .subtotal(subtotal)
                                .build());
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ Failed to parse item: {}", elem);
            }
        });

        return items;
    }

    // ==================== ERROR HANDLER ====================
    private InvoiceExtractionResult buildErrorResult(String errorMessage) {
        return InvoiceExtractionResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .rawResponse(null)
                .invoiceDate(null)
                .totalAmount(null)
                .vendor(null)
                .extractedItems(null)
                .build();
    }
}