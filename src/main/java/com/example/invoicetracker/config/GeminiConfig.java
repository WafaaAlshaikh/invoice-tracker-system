package com.example.invoicetracker.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Slf4j
@Configuration
@Getter
public class GeminiConfig {

    @Value("${google.ai.api.key:}")
    private String apiKey;

    @Value("${google.ai.model:gemini-1.5-flash}")
    private String modelName;

    @Value("${google.ai.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getApiEndpoint() {
        String endpoint = String.format("%s/models/%s:generateContent", baseUrl, modelName);
        log.info("🔧 Gemini API Endpoint: {}", endpoint);
        log.info("🔧 API Key configured: {}", apiKey != null && !apiKey.isEmpty());
        log.info("🔧 Model: {}", modelName);
        return endpoint;
    }
}