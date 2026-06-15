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

            // Build action format instructions as a SYSTEM prompt
            String systemInstructions = "You are a professional AI Business Assistant for a Grocery Shop.\n" 
                    + langInstruction + "\n\n"
                    + "=== ACTION INSTRUCTIONS (CRITICAL) ===\n"
                    + "If the user asks you to modify data (add/delete/update) or fetch reports, you MUST execute the action by outputting a JSON block EXACTLY like the examples below.\n"
                    + "If you say you did the action but do NOT output the JSON block, the backend WILL NOT execute it and you will be lying to the user!\n"
                    + "RULES:\n"
                    + "1. The JSON block MUST be at the VERY END of your response.\n"
                    + "2. Format: ```json\n{\"action\": \"action_name\", ...params}\n```\n"
                    + "3. Do NOT say 'Here is the action' or mention the JSON block in your text. Just answer normally and quietly append the block.\n"
                    + "4. IMPORTANT: Our chat is STATELESS. You DO NOT HAVE MEMORY of previous turns! Therefore, you MUST NEVER ask the user for confirmation!\n"
                    + "5. If you do NOT have the data in your Context and must fetch it via an action (like get_customer), DO NOT write a pessimistic conversational guess saying you don't know! Just say 'Fetching the requested details...' and append the JSON action block.\n"
                    + "6. If the user asks for data or a report that is NOT strictly mapped to one of the actions listed below (e.g. GST Report), DO NOT guess an action name! Just politely state you don't have the capability to fetch that specific report yet and DO NOT output any JSON block.\n"
                    + "7. If you have the REQUIRED details (like name and phone for a customer), you MUST execute the action IMMEDIATELY in the current response!\n\n"
                    + "CUSTOMER OPERATIONS:\n"
                    + "• Create Customer: ```json\n{\"action\": \"create_customer\", \"name\": \"John Doe\", \"phone\": \"9876543210\", \"address\": \"Mumbai\"}\n```\n"
                    + "• Delete Customer: ```json\n{\"action\": \"delete_customer\", \"customer_name\": \"John\"}\n```\n"
                    + "• Clear Udhari: ```json\n{\"action\": \"clear_udhari\", \"customer_name\": \"Harshal\"}\n```\n"
                    + "• Pay Udhari <₹10k: ```json\n{\"action\": \"pay_udhari\", \"customer_name\": \"Harshal\", \"amount\": 1000}\n```\n"
                    + "• Update Balance: ```json\n{\"action\": \"update_customer_balance\", \"customer_name\": \"Harshal\", \"balance\": 5000}\n```\n"
                    + "• Customers with Udhari: ```json\n{\"action\": \"list_customers_with_udhari\"}\n```\n"
                    + "• Get Customer: ```json\n{\"action\": \"get_customer\", \"customer_name\": \"Harshal\"}\n```\n"
                    + "• List Customers: ```json\n{\"action\": \"list_customers\"}\n```\n\n"
                    + "PRODUCTS:\n"
                    + "• Add Product: ```json\n{\"action\": \"create_product\", \"name_en\": \"New Item\", \"price\": 100, \"stock_qty\": 50, \"category\": \"General\"}\n```\n"
                    + "• Delete Product: ```json\n{\"action\": \"delete_product\", \"product_name\": \"Rice\"}\n```\n"
                    + "• Update Price: ```json\n{\"action\": \"update_product_price\", \"product_name\": \"Rice\", \"price\": 120}\n```\n"
                    + "• Update Stock: ```json\n{\"action\": \"update_stock\", \"product_name\": \"Rice\", \"stock_qty\": 50, \"operation\": \"set\"}\n```\n"
                    + "• Low Stock: ```json\n{\"action\": \"get_low_stock\", \"threshold\": 10}\n```\n"
                    + "• List/Top Products: ```json\n{\"action\": \"list_products\"}\n``` / ```json\n{\"action\": \"get_top_products\"}\n```\n\n"
                    + "SUPPLIERS:\n"
                    + "• Draft PO: ```json\n{\"action\": \"draft_purchase_order\", \"supplier_name\": \"ABC Traders\"}\n```\n"
                    + "• Create Supplier: ```json\n{\"action\": \"create_supplier\", \"name\": \"ABC Traders\", \"phone\": \"9999999999\", \"address\": \"Pune\", \"gst\": \"27AAAAA0000A1Z5\"}\n```\n"
                    + "• Delete Supplier: ```json\n{\"action\": \"delete_supplier\", \"supplier_name\": \"ABC Traders\"}\n```\n"
                    + "• Record Payment: ```json\n{\"action\": \"record_supplier_payment\", \"supplier_name\": \"ABC Traders\", \"amount\": 5000}\n```\n"
                    + "• List Suppliers: ```json\n{\"action\": \"list_suppliers\"}\n```\n\n"
                    + "REPORTS:\n"
                    + "• Dashboard: ```json\n{\"action\": \"get_dashboard_summary\"}\n```\n"
                    + "• Credit Report: ```json\n{\"action\": \"get_credit_report\"}\n```\n"
                    + "• Sales Report: ```json\n{\"action\": \"get_sales_report\"}\n```\n"
                    + "• Profit Report: ```json\n{\"action\": \"get_profit_report\"}\n```\n"
                    + "• Inventory Status: ```json\n{\"action\": \"get_inventory_status\"}\n```\n"
                    + "• Top Customers: ```json\n{\"action\": \"get_top_customers\"}\n```\n\n";

            // Build user prompt
            String userPrompt = "=== DATABASE CONTEXT ===\n"
                    + context
                    + "\n\n=== USER QUESTION ===\n"
                    + userQuestion;

            // Build request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 1024);

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemInstructions);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);

            requestBody.put("messages", List.of(systemMessage, userMessage));

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
            String msg = (String) actionResult.get("message");
            if (msg != null && msg.toLowerCase().startsWith("unknown action")) {
                sb.append("⚠️ I apologize, but I don't have the internal capability to perform that specific report or action just yet.\n");
            } else {
                sb.append("❌ Error: ").append(msg).append("\n");
            }
        }

        return sb.toString();
    }

    // ─────────────────────────────────────────────
    // REMOVE JSON BLOCKS FROM RESPONSE
    // ─────────────────────────────────────────────
    private String removeJsonFromResponse(String response) {
        if (response == null) return "";

        // Remove ```json...``` blocks (using DOTALL mode so it matches across newlines)
        String clean = response.replaceAll("(?s)```json.*?```", "");
        
        // Remove standalone JSON blocks by robustly finding { and }
        int start = clean.indexOf("{");
        int end = clean.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            clean = clean.substring(0, start) + clean.substring(end + 1);
        }
        
        // Remove backticks if any are left behind
        clean = clean.replace("```", "");
        
        // Remove "JSON block:" or similar labels
        clean = clean.replaceAll("(?i)(json|action)\\s*(block|format)?:?\\s*", "").trim();

        return clean;
    }
}