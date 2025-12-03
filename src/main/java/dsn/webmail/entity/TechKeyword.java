package dsn.webmail.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tech_keyword")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String keyword; // "Spring Boot", "JWT", "Docker"

    private String category; // Backend, Frontend, DevOps, Database

    @Column(columnDefinition = "TEXT")
    private String description; // 기술 설명

    private String iconClass; // Font Awesome 아이콘 (예: "fab fa-java")

    @Builder.Default
    private Integer globalUsageCount = 0; // 전체 사용자 사용 횟수 (인기도)

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

    public void incrementUsage() {
        this.globalUsageCount++;
        this.updatedAt = LocalDateTime.now();
    }
}
