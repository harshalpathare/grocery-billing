package com.example.grocery_billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaudeRagService {

    private final RagDataService ragDataService;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.api.model}")
    private String model;

    private final OkHttpClient httpClient = new OkHttpClient
            .Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    // ─────────────────────────────────────────────
    // MAIN METHOD — Ask Groq with RAG context
    // ─────────────────────────────────────────────
    public String ask(String userQuestion) {
        try {
            // Step 1: Build context from real DB data
            String context = ragDataService.buildContext(userQuestion);

            // Step 2: Build full prompt
            String fullPrompt = context
                    + "\n\n=== QUESTION ===\n"
                    + userQuestion;

            // Step 3: Build request body for Groq API (OpenAI-compatible)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 1024);

            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", fullPrompt);
            requestBody.put("messages", List.of(message));

            String json = objectMapper.writeValueAsString(requestBody);

            // Step 4: Call Groq API
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("Groq API error: {} - Body: {}", response.code(), errorBody);
                    return "Sorry, I couldn't process your question. API Error: " + response.code();
                }

                String responseBody = response.body().string();
                Map<?, ?> responseMap = objectMapper.readValue(responseBody, Map.class);

                // Extract text from Groq response
                List<?> choices = (List<?>) responseMap.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> messageMap = (Map<?, ?>) choice.get("message");
                    if (messageMap != null) {
                        return (String) messageMap.get("content");
                    }
                }

                return "No response received.";
            }

        } catch (Exception e) {
            log.error("RAG error: {}", e.getMessage(), e);
            return "Sorry, an error occurred: " + e.getMessage();
        }
    }
}