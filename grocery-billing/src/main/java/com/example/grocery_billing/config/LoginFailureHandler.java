package com.example.grocery_billing.config;

import com.example.grocery_billing.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginFailureHandler
        implements AuthenticationFailureHandler {

    private final ActivityLogService activityLogService;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException {

        String username = request.getParameter("username");
        activityLogService.logLogin(username, false, request);
        response.sendRedirect("/login?error");
    }
}