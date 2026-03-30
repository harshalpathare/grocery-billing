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
    public Map<String, String> ask(
            @RequestBody Map<String, String> body) {

        String question = body.get("question");
        Map<String, String> result = new HashMap<>();

        if (question == null || question.isBlank()) {
            result.put("answer", "Please ask a question.");
            return result;
        }

        String answer = claudeRagService.ask(question);
        result.put("answer", answer);
        return result;
    }
}