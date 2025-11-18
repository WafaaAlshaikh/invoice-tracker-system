package com.example.invoicetracker.service.ai;

import com.example.invoicetracker.config.GeminiConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiConfig geminiConfig;
    private final OkHttpClient httpClient;
    private final Gson gson;

    
    public String analyzeImage(MultipartFile imageFile, String prompt) throws IOException {
        log.info("Analyzing image with Gemini Vision - File: {}, Size: {} bytes",
                imageFile.getOriginalFilename(), imageFile.getSize());

        if (imageFile.getSize() > 4 * 1024 * 1024) {
            throw new IOException("File size too large. Maximum 4MB allowed.");
        }

        byte[] imageBytes = imageFile.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        String mimeType = determineMimeType(imageFile.getContentType(), imageFile.getOriginalFilename());

        String jsonRequest = buildVisionRequest(prompt, base64Image, mimeType);

        log.debug("Sending request to Gemini API...");

        Request httpRequest = new Request.Builder()
                .url(geminiConfig.getApiEndpoint() + "?key=" + geminiConfig.getApiKey())
                .post(RequestBody.create(jsonRequest, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            String responseBody = response.body().string();

            if (!response.isSuccessful()) {
                log.error("Gemini API error: {} - {}", response.code(), responseBody);
                throw new IOException("Gemini API error: " + response.code() + " - " + responseBody);
            }

            log.debug("Gemini API response received successfully");
            return extractTextFromResponse(responseBody);
        }
    }
    
    
    private String extractTextFromResponse(String responseBody) {
        try {
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            if (jsonResponse.has("candidates") && jsonResponse.getAsJsonArray("candidates").size() > 0) {
                JsonObject candidate = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject();
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    if (content.has("parts") && content.getAsJsonArray("parts").size() > 0) {
                        JsonObject part = content.getAsJsonArray("parts").get(0).getAsJsonObject();
                        if (part.has("text")) {
                            return part.get("text").getAsString();
                        }
                    }
                }
            }

            log.warn("No text found in Gemini response");
            return "No response text found";

        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
            return "Error parsing response";
        }
    }

    private String determineMimeType(String contentType, String fileName) {
        if (contentType != null) return contentType;

        if (fileName != null) {
            if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg"))
                return "image/jpeg";
            if (fileName.toLowerCase().endsWith(".png")) return "image/png";
            if (fileName.toLowerCase().endsWith(".gif")) return "image/gif";
            if (fileName.toLowerCase().endsWith(".pdf")) return "application/pdf";
        }

        return "image/jpeg"; 
    }
    
    
    private String buildVisionRequest(String prompt, String base64Image, String mimeType) {
        JsonObject request = new JsonObject();

        JsonObject content = new JsonObject();

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);

        JsonObject imagePart = new JsonObject();
        JsonObject inlineData = new JsonObject();
        inlineData.addProperty("mime_type", mimeType);
        inlineData.addProperty("data", base64Image);
        imagePart.add("inline_data", inlineData);

        content.add("parts", gson.toJsonTree(List.of(textPart, imagePart)));
        request.add("contents", gson.toJsonTree(List.of(content)));

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.1);
        generationConfig.addProperty("maxOutputTokens", 2048); // Increased for larger invoices
        generationConfig.addProperty("topP", 0.8);
        generationConfig.addProperty("topK", 40);

        request.add("generationConfig", generationConfig);

        return gson.toJson(request);
    }
    public String analyzeText(String prompt) throws IOException {
    log.info("Analyzing text with Gemini API...");

    JsonObject request = new JsonObject();
    JsonObject content = new JsonObject();
    JsonObject textPart = new JsonObject();
    textPart.addProperty("text", prompt);
    content.add("parts", gson.toJsonTree(List.of(textPart)));
    request.add("contents", gson.toJsonTree(List.of(content)));

    JsonObject generationConfig = new JsonObject();
    generationConfig.addProperty("temperature", 0.1);
    generationConfig.addProperty("maxOutputTokens", 1000);
    request.add("generationConfig", generationConfig);

    String jsonRequest = gson.toJson(request);

    Request httpRequest = new Request.Builder()
            .url(geminiConfig.getApiEndpoint() + "?key=" + geminiConfig.getApiKey())
            .post(RequestBody.create(jsonRequest, MediaType.parse("application/json")))
            .build();

    try (Response response = httpClient.newCall(httpRequest).execute()) {
        if (!response.isSuccessful()) {
            throw new IOException("Gemini API error: " + response.code());
        }
        String responseBody = response.body().string();
        return extractTextFromResponse(responseBody);
    }
}


    

    public String generateText(String prompt) throws IOException {
        log.info("Generating text with prompt: {}", prompt.substring(0, Math.min(100, prompt.length())) + "...");
        
        JsonObject request = new JsonObject();
        
        JsonObject content = new JsonObject();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);
        content.add("parts", gson.toJsonTree(List.of(textPart)));
        
        request.add("contents", gson.toJsonTree(List.of(content)));
        
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.1);
        generationConfig.addProperty("maxOutputTokens", 1000);
        
        request.add("generationConfig", generationConfig);
        
        String jsonRequest = gson.toJson(request);
        
        Request httpRequest = new Request.Builder()
                .url(geminiConfig.getApiEndpoint() + "?key=" + geminiConfig.getApiKey())
                .post(RequestBody.create(jsonRequest, MediaType.parse("application/json")))
                .build();
        
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Gemini API error: " + response.code());
            }
            
            String responseBody = response.body().string();
            return extractTextFromResponse(responseBody);
        }
    }
}