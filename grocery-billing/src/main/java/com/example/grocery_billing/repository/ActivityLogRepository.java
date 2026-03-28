package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface ActivityLogRepository
        extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findTop50ByOrderByCreatedAtDesc();

    List<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId);
void deleteByUserId(Long userId);
    List<ActivityLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT a.username,
               COUNT(a) as totalActions,
               SUM(CASE WHEN a.action='CREATE' THEN 1 ELSE 0 END) as creates,
               SUM(CASE WHEN a.action='DELETE' THEN 1 ELSE 0 END) as deletes
        FROM ActivityLog a
        WHERE a.createdAt >= :from
        GROUP BY a.username
        ORDER BY totalActions DESC
    """)
    List<Object[]> getDailyPerformance(LocalDateTime from);

    @Query("""
        SELECT a FROM ActivityLog a
        WHERE a.user.id = :userId
        AND a.createdAt >= :from
        ORDER BY a.createdAt DESC
    """)
    List<ActivityLog> findByUserAndDate(Long userId, LocalDateTime from);
}