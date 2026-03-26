package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LoginHistoryRepository
        extends JpaRepository<LoginHistory, Long> {

    List<LoginHistory> findTop20ByOrderByLoginTimeDesc();

    List<LoginHistory> findByUserIdOrderByLoginTimeDesc(Long userId);

    // Find latest open session for logout tracking
    Optional<LoginHistory> findTopByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(
            Long userId);
}