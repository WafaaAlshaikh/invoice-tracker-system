package com.example.invoicetracker.service.ai;

import com.example.invoicetracker.config.GeminiConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleGeminiService {

    private final GeminiConfig geminiConfig;
    private final RestTemplate restTemplate;
    private final Gson gson;

    
    
    public String testApi() {
        try {
            String url = geminiConfig.getApiEndpoint() + "?key=" + geminiConfig.getApiKey();
            
            log.info("🔧 Testing Gemini API with URL: {}", url.replace(geminiConfig.getApiKey(), "***"));
            
            Map<String, Object> requestBody = new HashMap<>();
            
            Map<String, Object> content = new HashMap<>();
            Map<String, String> textPart = new HashMap<>();
            textPart.put("text", "Say 'Hello World' in Arabic");
            
            content.put("parts", new Object[]{textPart});
            requestBody.put("contents", new Object[]{content});
            
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.1);
            generationConfig.put("maxOutputTokens", 100);
            requestBody.put("generationConfig", generationConfig);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("🔧 Sending request to Gemini API...");
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Gemini API test SUCCESS");
                String responseText = extractTextFromResponse(response.getBody());
                log.info("✅ API Response: {}", responseText);
                return responseText;
            } else {
                log.error("❌ Gemini API test FAILED: {} - {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("API returned: " + response.getStatusCode() + " - " + response.getBody());
            }
            
        } catch (Exception e) {
            log.error("❌ Gemini API test error: {}", e.getMessage());
            throw new RuntimeException("Gemini API test failed: " + e.getMessage(), e);
        }
    }

    
    private String extractTextFromResponse(String responseBody) {
        try {
            log.debug("🔧 Raw API Response: {}", responseBody);
            
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            if (jsonResponse.has("candidates") && 
                jsonResponse.getAsJsonArray("candidates").size() > 0) {
                
                JsonObject candidate = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject();
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    if (content.has("parts") && content.getAsJsonArray("parts").size() > 0) {
                        String text = content.getAsJsonArray("parts").get(0).getAsJsonObject()
                                .get("text").getAsString();
                        log.debug("🔧 Extracted text: {}", text);
                        return text;
                    }
                }
            }
            
            log.warn("⚠️ No text found in Gemini response");
            return "No response text found";
            
        } catch (Exception e) {
            log.error("❌ Error parsing Gemini response: {}", e.getMessage());
            return "Error parsing response: " + e.getMessage();
        }
    }
}