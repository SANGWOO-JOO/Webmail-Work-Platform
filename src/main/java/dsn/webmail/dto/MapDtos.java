package dsn.webmail.dto;

import dsn.webmail.entity.UserReaction.ReactionType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class MapDtos {

    // ===== Request DTOs =====

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantSearchRequest {
        private String category;          // 기존 호환용 (전체 카테고리 문자열)
        private String categoryLevel1;    // 대분류 (음식점, 카페)
        private String categoryLevel2;    // 중분류 (한식, 중식, 일식 등)
        private String categoryLevel3;    // 소분류 (육류,고기, 해물,생선 등)
        private Integer radius = 500;
        private String sort = "distance";  // distance, rating
        private Integer page = 1;
        private Boolean favoritesOnly = false;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoriteRequest {
        private String restaurantId;
        private String memo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactionRequest {
        private ReactionType type;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisitRequest {
        private String memo;
    }

    // ===== Response DTOs =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantResponse {
        private String id;
        private String name;
        private String category;
        private String address;
        private String phone;
        private Integer distance;
        private Double latitude;
        private Double longitude;
        private Double rating;
        private Integer reviewCount;
        private String thumbnailUrl;
        private String placeUrl;
        private Boolean isFavorite;
        private String myReaction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantListResponse {
        private List<RestaurantResponse> restaurants;
        private Integer totalCount;
        private Integer page;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantDetailResponse {
        private String id;
        private String name;
        private String category;
        private String address;
        private String phone;
        private String openHours;
        private Double latitude;
        private Double longitude;
        private String naverPlaceUrl;
        private String kakaoPlaceUrl;
        private List<BlogReviewResponse> blogReviews;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlogReviewResponse {
        private String title;
        private String description;
        private String bloggerName;
        private String postDate;
        private String link;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialInfoResponse {
        private Long likes;
        private Long dislikes;
        private List<String> likedBy;
        private List<String> dislikedBy;
        private List<String> favoritedBy;
        private LocalDateTime myLastVisit;
        private List<VisitorInfo> recentVisitors;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisitorInfo {
        private String name;
        private LocalDateTime visitedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoriteResponse {
        private Long id;
        private RestaurantResponse restaurant;
        private String memo;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoriteListResponse {
        private List<FavoriteResponse> favorites;
        private Integer totalCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisitResponse {
        private Long id;
        private String restaurantId;
        private String restaurantName;
        private String memo;
        private LocalDateTime visitedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisitListResponse {
        private List<VisitResponse> visits;
        private Integer totalCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactionResponse {
        private Long likes;
        private Long dislikes;
        private List<String> likedBy;
        private String myReaction;
    }

    // ===== Kakao API Response DTOs =====

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoPlaceResponse {
        private List<KakaoPlace> documents;
        private KakaoMeta meta;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoPlace {
        private String id;
        private String place_name;
        private String category_name;
        private String category_group_code;
        private String category_group_name;
        private String phone;
        private String address_name;
        private String road_address_name;
        private String x;  // longitude
        private String y;  // latitude
        private String place_url;
        private String distance;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoMeta {
        private Integer total_count;
        private Integer pageable_count;
        private Boolean is_end;
    }

    // ===== Naver API Response DTOs =====

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NaverBlogResponse {
        private String lastBuildDate;
        private Integer total;
        private Integer start;
        private Integer display;
        private List<NaverBlogItem> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NaverBlogItem {
        private String title;
        private String link;
        private String description;
        private String bloggername;
        private String bloggerlink;
        private String postdate;
    }
}
