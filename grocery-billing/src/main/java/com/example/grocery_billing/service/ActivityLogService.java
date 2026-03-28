package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.ActivityLog;
import com.example.grocery_billing.entity.LoginHistory;
import com.example.grocery_billing.entity.User;
import com.example.grocery_billing.repository.ActivityLogRepository;
import com.example.grocery_billing.repository.LoginHistoryRepository;
import com.example.grocery_billing.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final UserRepository userRepository;

    // ─────────────────────────────────────────────
    // LOG ANY ACTION
    // ─────────────────────────────────────────────
    public void log(String action, String entityType,
                    Long entityId, String description,
                    HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                log.warn("Activity log skipped: not authenticated");
                return;
            }

            String username = auth.getName();
            User user = userRepository.findByUsername(username)
                    .orElse(null);
            if (user == null) {
                log.warn("Activity log skipped: user {} not found", username);
                return;
            }

            String ipAddress = request != null ? getClientIp(request) : "unknown";

            ActivityLog actLog = ActivityLog.builder()
                    .user(user)
                    .username(username)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .ipAddress(ipAddress)
                    .build();

            activityLogRepository.save(actLog);
            log.info("Activity logged: {} {} by {}", action, entityType, username);

        } catch (Exception e) {
            log.error("Failed to save activity log: {}", e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────
    // LOG LOGIN
    // ─────────────────────────────────────────────
    public void logLogin(String username, boolean success,
                         HttpServletRequest request) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElse(null);
            if (user == null) return;

            LoginHistory history = LoginHistory.builder()
                    .user(user)
                    .username(username)
                    .ipAddress(getClientIp(request))
                    .status(success
                            ? LoginHistory.Status.SUCCESS
                            : LoginHistory.Status.FAILED)
                    .build();

            loginHistoryRepository.save(history);

            // Also log to activity log
            if (success) {
                ActivityLog actLog = ActivityLog.builder()
                        .user(user)
                        .username(username)
                        .action("LOGIN")
                        .entityType("USER")
                        .entityId(user.getId())
                        .description("User logged in")
                        .ipAddress(getClientIp(request))
                        .build();
                activityLogRepository.save(actLog);
            }
        } catch (Exception e) {
            log.warn("Failed to save login history: {}",
                    e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // LOG LOGOUT
    // ─────────────────────────────────────────────
    public void logLogout(String username,
                          HttpServletRequest request) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElse(null);
            if (user == null) return;

            // Update logout time on latest open session
            loginHistoryRepository
                    .findTopByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(
                            user.getId())
                    .ifPresent(h -> {
                        h.setLogoutTime(LocalDateTime.now());
                        loginHistoryRepository.save(h);
                    });

            // Log to activity log
            ActivityLog actLog = ActivityLog.builder()
                    .user(user)
                    .username(username)
                    .action("LOGOUT")
                    .entityType("USER")
                    .entityId(user.getId())
                    .description("User logged out")
                    .ipAddress(getClientIp(request))
                    .build();
            activityLogRepository.save(actLog);

        } catch (Exception e) {
            log.warn("Failed to save logout: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // GET TODAY'S PERFORMANCE
    // ─────────────────────────────────────────────
    public List<Map<String, Object>> getTodayPerformance() {
        LocalDateTime startOfDay = LocalDate.now()
                .atStartOfDay();
        List<Object[]> rows = activityLogRepository
                .getDailyPerformance(startOfDay);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("username",     row[0]);
            map.put("totalActions", row[1]);
            map.put("creates",      row[2]);
            map.put("deletes",      row[3]);
            result.add(map);
        }
        return result;
    }

    // ─────────────────────────────────────────────
    // GET RECENT LOGS
    // ─────────────────────────────────────────────
    public List<ActivityLog> getRecentLogs() {
        return activityLogRepository
                .findTop50ByOrderByCreatedAtDesc();
    }

    public List<ActivityLog> getLogsByUser(Long userId) {
        return activityLogRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<LoginHistory> getRecentLogins() {
        return loginHistoryRepository
                .findTop20ByOrderByLoginTimeDesc();
    }

    // ─────────────────────────────────────────────
    // GET CURRENT USER
    // ─────────────────────────────────────────────
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return userRepository.findByUsername(auth.getName())
                .orElse(null);
    }

    // ─────────────────────────────────────────────
    // HELPER — Get real client IP
    // ─────────────────────────────────────────────
    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}