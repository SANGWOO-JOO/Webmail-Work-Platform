package dsn.webmail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "processed_mail", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id",
                "messageId" }), indexes = {
                                @Index(name = "idx_processed_mail_user_id", columnList = "user_id"),
                                @Index(name = "idx_processed_mail_message_id", columnList = "messageId"),
                                @Index(name = "idx_processed_mail_processed_at", columnList = "processedAt")
                })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedMail {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false)
        private AppUser user;

        @Column(nullable = false, length = 255)
        private String messageId;

        @Column(nullable = false)
        @Builder.Default
        private LocalDateTime processedAt = LocalDateTime.now();

        @Column(length = 500)
        private String subject;

        @Column(length = 200)
        private String fromAddress;

        @Column(columnDefinition = "LONGTEXT")
        private String content;

        // LLM 분석 결과 필드
        @Enumerated(EnumType.STRING)
        @Column(length = 20)
        private MailCategory category;

        @Column(columnDefinition = "TEXT")
        private String summary;

        private LocalDateTime analyzedAt;

        private Float categoryConfidence;

}