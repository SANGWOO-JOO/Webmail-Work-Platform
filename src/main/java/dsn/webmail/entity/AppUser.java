package dsn.webmail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_user", uniqueConstraints = @UniqueConstraint(columnNames = "email"), indexes = {
        @Index(name = "idx_app_user_email", columnList = "email"),
        @Index(name = "idx_app_user_status", columnList = "status"),
        @Index(name = "idx_app_user_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(length = 100)
    private String name;

    @Column(length = 400)
    private String encryptedPop3Password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime activatedAt;

    @Column(length = 20)
    private String slackUserId;

    // Slack 알람 수신 설정 (기본값: OFF)
    @Column(nullable = false)
    @Builder.Default
    private Boolean slackNotificationEnabled = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean mailPollingEnabled = false;

    @Column
    private LocalDateTime lastCheckedAt;

    @Column
    @Builder.Default
    private Integer failureCount = 0;

    @Column
    private LocalDateTime nextRetryAt;

    @Column(nullable = false, length = 60)
    private String passwordHash;

    @Column(nullable = false)
    @Builder.Default
    private Boolean webLoginEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer loginFailureCount = 0;

    @Column
    private LocalDateTime lastLoginAt;

    public enum Status {
        PENDING, ACTIVE, LOCKED
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    public enum Role {
        USER,
        ADMIN
    }

    // Custom setter with business logic
    public void setStatus(Status status) {
        this.status = status;
        if (status == Status.ACTIVE && this.activatedAt == null) {
            this.activatedAt = LocalDateTime.now();
        }
    }
}