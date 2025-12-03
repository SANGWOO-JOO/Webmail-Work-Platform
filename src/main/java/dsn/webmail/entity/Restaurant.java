package dsn.webmail.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "restaurant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {

    @Id
    private String id;  // Kakao place ID

    @Column(nullable = false)
    private String name;

    private String category;           // 원본: "음식점 > 한식 > 육류,고기"

    // 카테고리 레벨별 분리
    private String categoryLevel1;      // 대분류: "음식점"
    private String categoryLevel2;      // 중분류: "한식"
    private String categoryLevel3;      // 소분류: "육류,고기"

    private String address;

    private String phone;

    private Double latitude;

    private Double longitude;

    private Integer distance;  // 회사로부터 거리 (m)

    private String placeUrl;

    private String thumbnailUrl;

    private Double rating;

    private Integer reviewCount;

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime syncedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
