package com.example.grocery_billing.controller;

import com.example.grocery_billing.service.ClaudeRagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final ClaudeRagService claudeRagService;

    @PostMapping("/ask")
    public Map<String, Object> ask(
            @RequestBody Map<String, String> body) {

        String question = body.get("question");
        Map<String, Object> result = new HashMap<>();

        if (question == null || question.isBlank()) {
            result.put("answer", "Please ask a question.");
            return result;
        }

        return claudeRagService.ask(question);
    }
}