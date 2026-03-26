package com.example.grocery_billing.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String username;

    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status { SUCCESS, FAILED }

    @PrePersist
    protected void onCreate() {
        loginTime = LocalDateTime.now();
    }
}