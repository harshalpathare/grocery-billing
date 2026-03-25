package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Security uses this to load user by username at login
    Optional<User> findByUsername(@org.springframework.data.repository.query.Param("username") String username);

    // Check if username already exists (for registration)
    boolean existsByUsername(@org.springframework.data.repository.query.Param("username") String username);
}
