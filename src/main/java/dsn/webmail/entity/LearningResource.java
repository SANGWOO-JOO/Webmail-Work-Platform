package dsn.webmail.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "learning_resource")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private TechKeyword keyword;

    @Column(nullable = false)
    private String title; // 자료 제목

    @Column(nullable = false)
    private String url; // 링크 URL

    @Enumerated(EnumType.STRING)
    private ResourceType type; // OFFICIAL_DOC, BLOG, TUTORIAL, VIDEO

    private String source; // 출처 (예: "Baeldung", "우아한형제들 기술블로그")

    private String language; // ko, en

    private Integer difficulty; // 1: 입문, 2: 중급, 3: 고급

    @Column(columnDefinition = "TEXT")
    private String summary; // 요약 설명

    @Builder.Default
    private Integer viewCount = 0; // 조회수

    private Float rating; // 평점 (1.0~5.0)

    @Builder.Default
    private Boolean isRecommended = false; // 추천 자료 여부

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ResourceType {
        OFFICIAL_DOC, // 공식 문서
        BLOG, // 기술 블로그
        TUTORIAL, // 튜토리얼
        VIDEO // 영상
    }
}
