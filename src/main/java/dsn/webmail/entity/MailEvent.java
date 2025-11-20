package dsn.webmail.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "mail_event")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MailEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "message_id")
    private String sourceMessageId; // 원본 메일 추적용

    private String title; // 회의 제목
    private String dateTime; // "2025-11-26 14:00" 형태로 저장
    private String location; // 장소

    private Float confidence; // AI 신뢰도(0.0 ~ 1.0)

    @CreatedDate
    private LocalDateTime createdAt;
}
