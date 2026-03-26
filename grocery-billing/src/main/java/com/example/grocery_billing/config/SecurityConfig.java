package com.example.grocery_billing.config;

import com.example.grocery_billing.entity.User;
import com.example.grocery_billing.repository.UserRepository;
import com.example.grocery_billing.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService  userDetailsService;

    // ✅ Three new handlers for activity logging
    private final LoginSuccessHandler       loginSuccessHandler;
    private final LoginFailureHandler       loginFailureHandler;
    private final AppLogoutSuccessHandler   appLogoutSuccessHandler;

    // ── Password Encoder ─────────────────────────────────
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── Authentication Manager ───────────────────────────
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ── Security Filter Chain ────────────────────────────
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // ── URL Authorization Rules ──────────────────
                .authorizeHttpRequests(auth -> auth
                        // Public — no login needed
                        .requestMatchers(
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",
                                "/admin/users",
                                "/admin/promote/**"
                        ).permitAll()

                        // Admin only
                        .requestMatchers(
                                "/settings/**",
                                "/users/**",
                                "/activity/**"   // ✅ Activity log — admin only
                        ).hasRole("ADMIN")

                        // Everything else requires login
                        .anyRequest().authenticated()
                )

                // ── Login ────────────────────────────────────
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)  // ✅ logs login + redirects to /
                        .failureHandler(loginFailureHandler)  // ✅ logs failed login + redirects to /login?error
                        .permitAll()
                )

                // ── Logout ───────────────────────────────────
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(appLogoutSuccessHandler) // ✅ logs logout + redirects to /login?logout
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .clearAuthentication(true)
                        .permitAll()
                )

                // ── Remember Me ──────────────────────────────
                .rememberMe(remember -> remember
                        .key("groceryBillingRememberMeKey2024")
                        .tokenValiditySeconds(7 * 24 * 60 * 60)
                        .rememberMeParameter("remember-me")
                        .userDetailsService(userDetailsService)
                )

                // ── Session Management ───────────────────────
                .sessionManagement(session -> session
                        .maximumSessions(3)
                );

        return http.build();
    }

    // ── Create Default Admin on First Startup ────────────
    @Bean
    public CommandLineRunner createDefaultAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            if (userRepository.count() == 0) {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("Shop Administrator")
                        .role("ROLE_ADMIN")
                        .enabled(true)
                        .build();

                userRepository.save(admin);

                System.out.println("========================================");
                System.out.println("  DEFAULT ADMIN CREATED!");
                System.out.println("  Username: admin");
                System.out.println("  Password: admin123");
                System.out.println("  Please change password after login!");
                System.out.println("========================================");
            }
        };
    }
}