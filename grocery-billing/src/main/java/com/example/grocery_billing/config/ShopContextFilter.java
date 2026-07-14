package com.example.grocery_billing.config;

import com.example.grocery_billing.entity.User;
import com.example.grocery_billing.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * SHOP CONTEXT FILTER
 * Runs on every request after authentication.
 * Reads the logged-in user's shopId and role,
 * stores them in ShopContext (ThreadLocal) so
 * any service method can call ShopContext.getShopId().
 */
@Component
@RequiredArgsConstructor
public class ShopContextFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder
                    .getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {

                String username = auth.getName();
                userRepository.findByUsername(username).ifPresent(user -> {
                    String role = user.getRole();
                    if (role != null && !role.startsWith("ROLE_")) {
                        role = "ROLE_" + role.toUpperCase();
                    }
                    ShopContext.setRole(role);
                    if (user.getShop() != null) {
                        ShopContext.setShopId(user.getShop().getId());
                    }
                });
            }

            filterChain.doFilter(request, response);
        } finally {
            // Always clear after request — prevents data leaks between requests
            ShopContext.clear();
        }
    }
}
