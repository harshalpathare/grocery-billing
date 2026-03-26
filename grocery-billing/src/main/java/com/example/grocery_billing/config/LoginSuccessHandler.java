package com.example.grocery_billing.config;

import com.example.grocery_billing.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler
        implements AuthenticationSuccessHandler {

    private final ActivityLogService activityLogService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException {

        activityLogService.logLogin(
                authentication.getName(), true, request);
        response.sendRedirect("/");
    }
}