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
    private final ActionExecutorService actionExecutorService;
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
    // Returns: response with action execution results
    // ─────────────────────────────────────────────
    public Map<String, Object> ask(String userQuestion) {
        Map<String, Object> fullResult = new HashMap<>();

        try {
            // Check if user is confirming a previous action
            if (userQuestion.toLowerCase().trim().matches("yes|y|confirm|ok|proceed")) {
                fullResult.put("answer", "Please retry your previous action - it will execute now.");
                fullResult.put("awaiting_confirmation", false);
                return fullResult;
            }
            // Detect language
            String lang = ragDataService.detectLanguage(
                    userQuestion);

            // Build context
            String context = ragDataService
                    .buildContext(userQuestion);

            // Language instruction with action format guidance
            String langInstruction = switch (lang) {
                case "hi" -> "The user is asking in Hindi. "
                        + "Please respond in Hindi (Devanagari script). "
                        + "Use ₹ for currency and Indian number system "
                        + "(lakh, crore).";
                case "mr" -> "The user is asking in Marathi. "
                        + "Please respond in Marathi (Devanagari script). "
                        + "Use ₹ for currency and Indian number system.";
                default   -> "Please respond in English. "
                        + "Use ₹ for currency.";
            };

            // Build action format instructions
            String actionInstructions = "\n\n=== ACTION INSTRUCTIONS ===\n"
                    + "IMPORTANT: Always include EXACTLY ONE JSON block at the end of your response when an action is needed.\n"
                    + "Format: ```json\n{\"action\": \"action_name\", ...params}\n```\n\n"
                    + "CUSTOMER OPERATIONS (Execute directly, NO confirmation needed):\n"
                    + "• Clear Udhari: ```json\n{\"action\": \"clear_udhari\", \"customer_name\": \"Harshal\"}\n```\n"
                    + "• Pay Udhari <₹10k: ```json\n{\"action\": \"pay_udhari\", \"customer_name\": \"Harshal\", \"amount\": 1000}\n```\n"
                    + "• Customers with Udhari: ```json\n{\"action\": \"list_customers_with_udhari\"}\n```\n"
                    + "• Get Customer: ```json\n{\"action\": \"get_customer\", \"customer_name\": \"Harshal\"}\n```\n"
                    + "• List Customers: ```json\n{\"action\": \"list_customers\"}\n```\n\n"
                    + "PRODUCTS/SUPPLIERS (Execute directly):\n"
                    + "• Update Stock: ```json\n{\"action\": \"update_stock\", \"product_name\": \"Rice\", \"stock_qty\": 50, \"operation\": \"set\"}\n```\n"
                    + "• Low Stock: ```json\n{\"action\": \"get_low_stock\", \"threshold\": 10}\n```\n\n"
                    + "REPORTS (Always execute):\n"
                    + "• Dashboard: ```json\n{\"action\": \"get_dashboard_summary\"}\n```\n"
                    + "• Credit Report: ```json\n{\"action\": \"get_credit_report\"}\n```\n"
                    + "• Sales: ```json\n{\"action\": \"get_sales_report\"}\n```\n\n";

            // Build full prompt
            String fullPrompt = langInstruction + "\n\n"
                    + context
                    + actionInstructions
                    + "\n=== QUESTION ===\n"
                    + userQuestion;

            // Build request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 1024);

            Map<String, String> message = new HashMap<>();
            message.put("role",    "user");
            message.put("content", fullPrompt);
            requestBody.put("messages", List.of(message));

            String json = objectMapper
                    .writeValueAsString(requestBody);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(RequestBody.create(json,
                            MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient
                    .newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorMsg = lang.equals("hi")
                            ? "माफ करें, कोई त्रुटि हुई।"
                            : lang.equals("mr")
                            ? "माफ करा, एक त्रुटी झाली."
                            : "Sorry, an error occurred.";
                    fullResult.put("answer", errorMsg);
                    fullResult.put("error", true);
                    return fullResult;
                }

                String responseBody = response.body().string();
                Map<?, ?> responseMap = objectMapper
                        .readValue(responseBody, Map.class);
                List<?> choices =
                        (List<?>) responseMap.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<?, ?> first = (Map<?, ?>) choices.get(0);
                    Map<?, ?> msgContent = (Map<?, ?>) first.get("message");
                    String aiResponse = (String) msgContent.get("content");

                    fullResult.put("answer", aiResponse);

                    // Try to execute any action in the response
                    Map<String, Object> actionResult =
                            actionExecutorService.executeAction(aiResponse);

                    if ((Boolean) actionResult.getOrDefault("action_executed", false)) {
                        fullResult.put("action_executed", true);
                        fullResult.put("action_result", actionResult);

                        // Add formatted result to answer (without JSON)
                        String actionInfo = formatActionResult(actionResult);
                        String cleanAnswer = removeJsonFromResponse(aiResponse);
                        fullResult.put("answer", cleanAnswer + "\n\n" + actionInfo);

                        log.info("Action executed: {}", actionResult);
                    } else {
                        // Remove JSON from plain text responses too
                        String cleanAnswer = removeJsonFromResponse(aiResponse);
                        fullResult.put("answer", cleanAnswer);
                    }

                    return fullResult;
                }

                fullResult.put("answer", "No response.");
                return fullResult;
            }
        } catch (Exception e) {
            log.error("RAG error: {}", e.getMessage(), e);
            fullResult.put("answer", "Error: " + e.getMessage());
            fullResult.put("error", true);
            return fullResult;
        }
    }

    // ─────────────────────────────────────────────
    // FORMAT ACTION RESULT FOR DISPLAY
    // ─────────────────────────────────────────────
    private String formatActionResult(Map<String, Object> actionResult) {
        StringBuilder sb = new StringBuilder();

        if ((Boolean) actionResult.getOrDefault("success", false)) {
            String message = (String) actionResult.get("message");
            if (message != null) {
                sb.append("✅ ").append(message).append("\n");
            }

            // Show relevant details
            if (actionResult.containsKey("customer_name")) {
                sb.append("👤 Customer: ").append(actionResult.get("customer_name")).append("\n");
            }
            if (actionResult.containsKey("new_balance")) {
                sb.append("💰 New Balance: ₹").append(actionResult.get("new_balance")).append("\n");
            }
            if (actionResult.containsKey("previous_balance")) {
                sb.append("📊 Previous Balance: ₹").append(actionResult.get("previous_balance")).append("\n");
            }
            if (actionResult.containsKey("amount_paid")) {
                sb.append("💳 Amount Paid: ₹").append(actionResult.get("amount_paid")).append("\n");
            }
        } else {
            sb.append("❌ Error: ").append(actionResult.get("message")).append("\n");
        }

        return sb.toString();
    }

    // ─────────────────────────────────────────────
    // REMOVE JSON BLOCKS FROM RESPONSE
    // ─────────────────────────────────────────────
    private String removeJsonFromResponse(String response) {
        if (response == null) return "";

        // Remove ```json...``` blocks
        String clean = response.replaceAll("```json\\s*\\{[^}]*\\}\\s*```", "").trim();
        // Remove standalone JSON blocks
        clean = clean.replaceAll("\\{\"action\"[^}]*\\}", "").trim();
        // Remove "JSON block:" or similar labels
        clean = clean.replaceAll("(?i)(json|action)\\s*(block|format)?:?\\s*", "").trim();

        return clean;
    }
}