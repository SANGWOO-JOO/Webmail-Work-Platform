package dsn.webmail.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "processed_mail",
       uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "messageId"}),
       indexes = {
           @Index(name = "idx_processed_mail_user_id", columnList = "userId"),
           @Index(name = "idx_processed_mail_message_id", columnList = "messageId"),
           @Index(name = "idx_processed_mail_processed_at", columnList = "processedAt")
       })
public class ProcessedMail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String messageId;

    @Column(nullable = false)
    private LocalDateTime processedAt = LocalDateTime.now();

    @Column(length = 500)
    private String subject;

    @Column(length = 200)
    private String fromAddress;

    public ProcessedMail() {}

    public ProcessedMail(Long userId, String messageId) {
        this.userId = userId;
        this.messageId = messageId;
    }

    public ProcessedMail(Long userId, String messageId, String subject, String fromAddress) {
        this.userId = userId;
        this.messageId = messageId;
        this.subject = subject;
        this.fromAddress = fromAddress;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }
}