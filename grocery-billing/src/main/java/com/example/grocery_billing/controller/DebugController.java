package com.example.grocery_billing.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;

@RestController
public class DebugController {

    @GetMapping("/debug/paths")
    public Map<String, String> debugPaths(HttpServletRequest request) {
        Map<String, String> paths = new HashMap<>();
        paths.put("requestURI", request.getRequestURI());
        paths.put("servletPath", request.getServletPath());
        paths.put("pathInfo", request.getPathInfo());
        paths.put("contextPath", request.getContextPath());
        return paths;
    }
}
