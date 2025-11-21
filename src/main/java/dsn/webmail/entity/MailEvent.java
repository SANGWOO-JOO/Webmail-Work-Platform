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

    // 수동 생성 관련 필드
    @Column(name = "is_manual")
    private Boolean isManual; // 수동 생성 여부 (true: 수동, false: AI 추출)

    @Column(columnDefinition = "TEXT")
    private String description; // 일정 상세 설명

    private String category; // 카테고리 (개발, 회의, 배포, 리뷰, 학습, 기타)

    private String priority; // 우선순위 (HIGH, MEDIUM, LOW)

    @Column(name = "related_link")
    private String relatedLink; // 관련 링크 (GitHub, Jira 등)

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정 일시

    private String color; // 일정 색상 (HEX 코드, 예: #3182F6)
}
