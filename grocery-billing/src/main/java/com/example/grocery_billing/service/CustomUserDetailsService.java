package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.User;
import com.example.grocery_billing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CUSTOM USER DETAILS SERVICE
 *
 * Spring Security calls loadUserByUsername() during login.
 * We load the user from our database and return a UserDetails object.
 *
 * Spring Security then compares the password hash automatically.
 * We never compare passwords manually.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Login attempt with unknown username: {}", username);
                    return new UsernameNotFoundException(
                            "User not found: " + username);
                });

        if (!user.getEnabled()) {
            throw new DisabledException("Account is disabled: " + username);
        }

        // Update last login time
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("User logged in: {}", username);

        // Return Spring Security's UserDetails object
        // Spring Security uses this to verify the password
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(user.getRole()))
        );
    }
}