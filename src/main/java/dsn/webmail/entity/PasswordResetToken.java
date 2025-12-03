package dsn.webmail.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String codeHash;  // 인증코드 해시

    @Column(nullable = false)
    private LocalDateTime expiresAt;  // 만료시간

    @Column(nullable = false)
    @Builder.Default
    private int attemptCount = 0;  // 시도 횟수

    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;  // 인증 완료 여부

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void incrementAttemptCount() {
        this.attemptCount++;
    }
}
