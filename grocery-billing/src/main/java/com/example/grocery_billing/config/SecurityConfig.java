package com.example.grocery_billing.config;

import com.example.grocery_billing.entity.Shop;
import com.example.grocery_billing.entity.User;
import com.example.grocery_billing.repository.ShopRepository;
import com.example.grocery_billing.repository.UserRepository;
import com.example.grocery_billing.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDate;

/**
 * SECURITY CONFIGURATION
 *
 * Roles:
 *   ROLE_SUPER_ADMIN → full platform access (all shops)
 *   ROLE_OWNER       → full access to own shop
 *   ROLE_CASHIER     → billing only (own shop)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final ShopContextFilter shopContextFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, com.example.grocery_billing.security.JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
            .securityMatcher("/api/v1/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                // Public
                .requestMatchers(
                    "/login",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico"
                ).permitAll()

                // Super admin only — platform management
                .requestMatchers(
                    "/super/**",
                    "/admin/shops/**"
                ).hasAnyAuthority("ROLE_SUPER_ADMIN")

                // Owner + Super admin — settings and user management
                .requestMatchers(
                    "/settings/**",
                    "/users/**"
                ).hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_OWNER", "ROLE_ADMIN")

                // Cashier restricted — no reports, no customers management
                .requestMatchers(
                    "/reports/**",
                    "/customers/new",
                    "/customers/*/edit",
                    "/products/new",
                    "/products/*/edit",
                    "/products/*/delete",
                    "/suppliers/**",
                    "/purchases/**",
                    "/credit/**"
                ).hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_OWNER", "ROLE_ADMIN")

                // Everything else — any authenticated user
                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .clearAuthentication(true)
                .permitAll()
            )

            .rememberMe(remember -> remember
                .key("groceryBillingRememberMeKey2024")
                .tokenValiditySeconds(7 * 24 * 60 * 60)
                .rememberMeParameter("remember-me")
                .userDetailsService(userDetailsService)
            )

            .sessionManagement(session -> session
                .maximumSessions(5)
            )

            // Register ShopContextFilter AFTER anonymous filter so Remember-Me is also processed
            .addFilterAfter(shopContextFilter,
                org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BOOTSTRAP ON FIRST STARTUP
     * Creates the default super admin + a demo shop with owner account.
     * Runs only when the users table is empty.
     */
    @Bean
    public CommandLineRunner bootstrapData(
            UserRepository userRepository,
            ShopRepository shopRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {

        return args -> {
            if (!userRepository.existsByUsername("superadmin")) {
                // ── 1. Create SUPER_ADMIN ─────────────────────
                User superAdmin = User.builder()
                    .username("superadmin")
                    .password(passwordEncoder.encode("super@admin123"))
                    .fullName("Platform Administrator")
                    .role("ROLE_SUPER_ADMIN")
                    .enabled(true)
                    .build();
                userRepository.save(superAdmin);
                
                System.out.println("╔══════════════════════════════════════════╗");
                System.out.println("║        SUPER ADMIN ACCOUNT CREATED        ║");
                System.out.println("╠══════════════════════════════════════════╣");
                System.out.println("║    Username : superadmin                  ║");
                System.out.println("║    Password : super@admin123              ║");
                System.out.println("╚══════════════════════════════════════════╝");
            }
            
            // ── 2. Fix or Create Demo Shop and Admin User ───────────────
            Shop demoShop = shopRepository.findById(1L).orElse(null);
            
            if (demoShop == null) {
                demoShop = Shop.builder()
                    .shopName("My Grocery Store")
                    .ownerName("Shop Owner")
                    .phone("9876543210")
                    .gstin("27AABCU9603R1ZX")
                    .thankYouMsg("Thank you for shopping with us! Visit again.")
                    .businessType("GROCERY")
                    .active(true)
                    .subscriptionEndDate(LocalDate.now().plusYears(1))
                    .build();
                demoShop = shopRepository.save(demoShop);
            }

            if (!userRepository.existsByUsername("admin")) {
                // Create OWNER for demo shop if doesn't exist
                User owner = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Shop Administrator")
                    .role("ROLE_OWNER")
                    .shop(demoShop)
                    .enabled(true)
                    .build();
                userRepository.save(owner);

                System.out.println("╔══════════════════════════════════════════╗");
                System.out.println("║        DEMO SHOP ACCOUNT CREATED          ║");
                System.out.println("╠══════════════════════════════════════════╣");
                System.out.println("║    Username : admin                       ║");
                System.out.println("║    Password : admin123                    ║");
                System.out.println("╚══════════════════════════════════════════╝");
            } else {
                // Fix Phase 1 admin if missing shop
                User adminUser = userRepository.findByUsername("admin").get();
                boolean updated = false;
                if (adminUser.getShop() == null) {
                    adminUser.setShop(demoShop);
                    updated = true;
                }
                if (!"ROLE_OWNER".equals(adminUser.getRole())) {
                    adminUser.setRole("ROLE_OWNER");
                    updated = true;
                }
                if (updated) {
                    userRepository.save(adminUser);
                    System.out.println("✅ Fixed legacy 'admin' user (assigned to demo shop and updated role)");
                }
            }

            // ── 3. Data Migration for Phase 1 & 2 ────────────────
            // Fix any old records that were created before Phase 3 (when shop_id didn't exist)
            try {
                Long defaultShopId = demoShop.getId();
                String[] tables = {"products", "suppliers", "customers", "purchase_orders", "bills"};
                for (String table : tables) {
                    int updatedRows = jdbcTemplate.update(
                        "UPDATE " + table + " SET shop_id = ? WHERE shop_id IS NULL", 
                        defaultShopId
                    );
                    if (updatedRows > 0) {
                        System.out.println("✅ Migrated " + updatedRows + " legacy records in '" + table + "' to Demo Shop");
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️ Could not run data migration: " + e.getMessage());
            }
        };
    }
}
