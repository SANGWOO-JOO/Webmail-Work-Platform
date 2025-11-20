package dsn.webmail.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_user", 
       uniqueConstraints = @UniqueConstraint(columnNames = "email"),
       indexes = {
           @Index(name = "idx_app_user_email", columnList = "email"),
           @Index(name = "idx_app_user_status", columnList = "status"),
           @Index(name = "idx_app_user_created_at", columnList = "createdAt")
       })
public class AppUser {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(length = 400)
    private String encryptedPop3Password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime activatedAt;

    @Column(length = 20)
    private String slackUserId;

    @Column(nullable = false)
    private Boolean mailPollingEnabled = false;

    @Column
    private LocalDateTime lastCheckedAt;

    @Column
    private Integer failureCount = 0;

    @Column
    private LocalDateTime nextRetryAt;

    @Column(nullable = false, length = 60)
    private String passwordHash;

    @Column(nullable = false)
    private Boolean webLoginEnabled = true;

    @Column(nullable = false)
    private Integer loginFailureCount = 0;

    @Column
    private LocalDateTime lastLoginAt;

    public enum Status {
        PENDING, ACTIVE, LOCKED
    }
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    public enum Role {
        USER,
        ADMIN
    }

    // Getter & Setter
    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }


    public AppUser() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEncryptedPop3Password() {
        return encryptedPop3Password;
    }

    public void setEncryptedPop3Password(String encryptedPop3Password) {
        this.encryptedPop3Password = encryptedPop3Password;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        if (status == Status.ACTIVE && this.activatedAt == null) {
            this.activatedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public String getSlackUserId() {
        return slackUserId;
    }

    public void setSlackUserId(String slackUserId) {
        this.slackUserId = slackUserId;
    }

    public Boolean getMailPollingEnabled() {
        return mailPollingEnabled;
    }

    public void setMailPollingEnabled(Boolean mailPollingEnabled) {
        this.mailPollingEnabled = mailPollingEnabled;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Boolean getWebLoginEnabled() {
        return webLoginEnabled;
    }

    public void setWebLoginEnabled(Boolean webLoginEnabled) {
        this.webLoginEnabled = webLoginEnabled;
    }

    public Integer getLoginFailureCount() {
        return loginFailureCount;
    }

    public void setLoginFailureCount(Integer loginFailureCount) {
        this.loginFailureCount = loginFailureCount;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}