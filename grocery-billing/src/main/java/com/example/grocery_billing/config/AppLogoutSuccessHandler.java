package com.example.grocery_billing.config;

import com.example.grocery_billing.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AppLogoutSuccessHandler
        implements LogoutSuccessHandler {

    private final ActivityLogService activityLogService;

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException {

        if (authentication != null) {
            activityLogService.logLogout(
                    authentication.getName(), request);
        }
        response.sendRedirect("/login?logout");
    }
}