package dsn.webmail.service;

import dsn.webmail.dto.MapDtos.*;
import dsn.webmail.entity.*;
import dsn.webmail.entity.UserReaction.ReactionType;
import dsn.webmail.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MapService {

    private final RestaurantRepository restaurantRepository;
    private final UserFavoriteRestaurantRepository favoriteRepository;
    private final UserReactionRepository reactionRepository;
    private final UserVisitRepository visitRepository;
    private final KakaoLocalApiClient kakaoApiClient;
    private final NaverSearchApiClient naverApiClient;

    // 반경 구간 정의 (미터) - Kakao API pageable_count=45 제한 우회를 위한 분할 전략
    private static final int[] RADIUS_RANGES = {200, 400, 700, 1000};

    // ===== 맛집 목록 조회 =====

    @Transactional(readOnly = true)
    public RestaurantListResponse getRestaurants(AppUser user, RestaurantSearchRequest request) {
        List<Restaurant> restaurants;

        // 카테고리 레벨 필터가 있으면 새 쿼리 사용, 없으면 기존 호환 쿼리 사용
        boolean useCategoryLevels = request.getCategoryLevel1() != null ||
                                    request.getCategoryLevel2() != null ||
                                    request.getCategoryLevel3() != null;

        if (useCategoryLevels) {
            // 카테고리 레벨별 필터 사용
            if ("rating".equals(request.getSort())) {
                restaurants = restaurantRepository.findByCategoryLevelsOrderByRating(
                        request.getCategoryLevel1(),
                        request.getCategoryLevel2(),
                        request.getCategoryLevel3(),
                        request.getRadius());
            } else {
                restaurants = restaurantRepository.findByCategoryLevels(
                        request.getCategoryLevel1(),
                        request.getCategoryLevel2(),
                        request.getCategoryLevel3(),
                        request.getRadius());
            }
        } else {
            // 기존 호환: category 문자열 필터 사용
            if ("rating".equals(request.getSort())) {
                restaurants = restaurantRepository.findByFiltersOrderByRating(
                        request.getCategory(), request.getRadius());
            } else {
                restaurants = restaurantRepository.findByFilters(
                        request.getCategory(), request.getRadius());
            }
        }

        // 즐겨찾기만 보기
        Set<String> favoriteIds = favoriteRepository.findRestaurantIdsByUser(user)
                .stream().collect(Collectors.toSet());

        if (Boolean.TRUE.equals(request.getFavoritesOnly())) {
            restaurants = restaurants.stream()
                    .filter(r -> favoriteIds.contains(r.getId()))
                    .collect(Collectors.toList());
        }

        // 사용자의 반응 정보
        List<RestaurantResponse> responses = restaurants.stream()
                .map(r -> toRestaurantResponse(r, user, favoriteIds))
                .collect(Collectors.toList());

        return RestaurantListResponse.builder()
                .restaurants(responses)
                .totalCount(responses.size())
                .page(request.getPage())
                .build();
    }

    private RestaurantResponse toRestaurantResponse(Restaurant r, AppUser user, Set<String> favoriteIds) {
        String myReaction = null;
        Optional<UserReaction> reaction = reactionRepository.findByUserAndRestaurant(user, r);
        if (reaction.isPresent()) {
            myReaction = reaction.get().getType().name();
        }

        return RestaurantResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .category(r.getCategory())
                .address(r.getAddress())
                .phone(r.getPhone())
                .distance(r.getDistance())
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .rating(r.getRating())
                .reviewCount(r.getReviewCount())
                .thumbnailUrl(r.getThumbnailUrl())
                .placeUrl(r.getPlaceUrl())
                .isFavorite(favoriteIds.contains(r.getId()))
                .myReaction(myReaction)
                .build();
    }

    // ===== 맛집 상세 정보 =====

    @Transactional(readOnly = true)
    public RestaurantDetailResponse getRestaurantDetail(String restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("식당을 찾을 수 없습니다."));

        // Naver 블로그 리뷰 조회
        NaverBlogResponse blogResponse = naverApiClient.searchRestaurantReviews(
                restaurant.getName(), restaurant.getAddress());

        List<BlogReviewResponse> blogReviews = new ArrayList<>();
        if (blogResponse != null && blogResponse.getItems() != null) {
            blogReviews = blogResponse.getItems().stream()
                    .map(item -> BlogReviewResponse.builder()
                            .title(removeHtmlTags(item.getTitle()))
                            .description(removeHtmlTags(item.getDescription()))
                            .bloggerName(item.getBloggername())
                            .postDate(formatDate(item.getPostdate()))
                            .link(item.getLink())
                            .build())
                    .collect(Collectors.toList());
        }

        return RestaurantDetailResponse.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .category(restaurant.getCategory())
                .address(restaurant.getAddress())
                .phone(restaurant.getPhone())
                .latitude(restaurant.getLatitude())
                .longitude(restaurant.getLongitude())
                .kakaoPlaceUrl(restaurant.getPlaceUrl())
                .blogReviews(blogReviews)
                .build();
    }

    // ===== 사회적 정보 조회 =====

    @Transactional(readOnly = true)
    public SocialInfoResponse getSocialInfo(AppUser user, String restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("식당을 찾을 수 없습니다."));

        long likes = reactionRepository.countByRestaurantAndType(restaurant, ReactionType.LIKE);
        long dislikes = reactionRepository.countByRestaurantAndType(restaurant, ReactionType.DISLIKE);

        List<String> likedBy = reactionRepository.findUserNamesByRestaurantAndType(
                restaurant, ReactionType.LIKE);
        List<String> dislikedBy = reactionRepository.findUserNamesByRestaurantAndType(
                restaurant, ReactionType.DISLIKE);
        List<String> favoritedBy = favoriteRepository.findUserNamesByRestaurant(restaurant);

        // 내 마지막 방문
        LocalDateTime myLastVisit = null;
        List<UserVisit> myVisits = visitRepository.findByUserAndRestaurantOrderByVisitedAtDesc(user, restaurant);
        if (!myVisits.isEmpty()) {
            myLastVisit = myVisits.get(0).getVisitedAt();
        }

        // 최근 방문자
        List<UserVisit> recentVisits = visitRepository.findRecentVisitorsByRestaurant(restaurant);
        List<VisitorInfo> recentVisitors = recentVisits.stream()
                .map(v -> VisitorInfo.builder()
                        .name(v.getUser().getName() != null ? v.getUser().getName() : v.getUser().getEmail())
                        .visitedAt(v.getVisitedAt())
                        .build())
                .collect(Collectors.toList());

        return SocialInfoResponse.builder()
                .likes(likes)
                .dislikes(dislikes)
                .likedBy(likedBy)
                .dislikedBy(dislikedBy)
                .favoritedBy(favoritedBy)
                .myLastVisit(myLastVisit)
                .recentVisitors(recentVisitors)
                .build();
    }

    // ===== 즐겨찾기 =====

    @Transactional
    public void addFavorite(AppUser user, FavoriteRequest request) {
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new RuntimeException("식당을 찾을 수 없습니다."));

        if (favoriteRepository.existsByUserAndRestaurant(user, restaurant)) {
            throw new RuntimeException("이미 즐겨찾기에 추가된 식당입니다.");
        }

        UserFavoriteRestaurant favorite = UserFavoriteRestaurant.builder()
                .user(user)
                .restaurant(restaurant)
                .memo(request.getMemo())
                .build();

        favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(AppUser user, String restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("식당을 찾을 수 없습니다."));

        favoriteRepository.deleteByUserAndRestaurant(user, restaurant);
    }

    @Transactional(readOnly = true)
    public FavoriteListResponse getFavorites(AppUser user) {
        List<UserFavoriteRestaurant> favorites = favoriteRepository.findByUserOrderByCreatedAtDesc(user);

        Set<String> favoriteIds = favorites.stream()
                .map(f -> f.getRestaurant().getId())
                .collect(Collectors.toSet());

        List<FavoriteResponse> responses = favorites.stream()
                .map(f -> FavoriteResponse.builder()
                        .id(f.getId())
                        .restaurant(toRestaurantResponse(f.getRestaurant(), user, favoriteIds))
                        .memo(f.getMemo())
                        .createdAt(f.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return FavoriteListResponse.builder()
                .favorites(responses)
                .totalCount(responses.size())
                .build();
    }

    // ===== 좋아요/싫어요 =====

    @Transactional
    public ReactionResponse addReaction(AppUser user, String restaurantId, ReactionRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("식당을 찾을 수 없습니다."));

        Optional<UserReaction> existingReaction = reactionRepository.findByUserAndRestaurant(user, restaurant);

        String newReaction = null;

        if (existingReaction.isPresent()) {
            ReactionType existingType = existingReaction.get().getType();
            // 기존 반응 삭제 후 즉시 DB 반영 (flush)
            reactionRepository.delete(existingReaction.get());
            reactionRepository.flush();

            // 같은 타입이면 토글 (취소)
            if (existingType == request.getType()) {
                // 취소만 하고 새로 추가하지 않음
                newReaction = null;
            } else {
                // 다른 타입이면 새 반응 추가
                UserReaction reaction = UserReaction.builder()
                        .user(user)
                        .restaurant(restaurant)
                        .type(request.getType())
                        .build();
                reactionRepository.save(reaction);
                newReaction = request.getType().name();
            }
        } else {
            // 기존 반응 없으면 새로 추가
            UserReaction reaction = UserReaction.builder()
                    .user(user)
                    .restaurant(restaurant)
                    .type(request.getType())
                    .build();
            reactionRepository.save(reaction);
            newReaction = request.getType().name();
        }

        // 업데이트된 카운트 반환
        long likes = reactionRepository.countByRestaurantAndType(restaurant, ReactionType.LIKE);
        long dislikes = reactionRepository.countByRestaurantAndType(restaurant, ReactionType.DISLIKE);
        List<String> likedBy = reactionRepository.findUserNamesByRestaurantAndType(restaurant, ReactionType.LIKE);

        return ReactionResponse.builder()
                .likes(likes)
                .dislikes(dislikes)
                .likedBy(likedBy)
                .myReaction(newReaction)
                .build();
    }

    @Transactional
    public void removeReaction(AppUser user, String restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("식당을 찾을 수 없습니다."));

        reactionRepository.deleteByUserAndRestaurant(user, restaurant);
    }

    // ===== 방문 기록 =====

    @Transactional
    public void addVisit(AppUser user, String restaurantId, VisitRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("식당을 찾을 수 없습니다."));

        // 같은 날 중복 방문 기록 체크
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        if (visitRepository.existsByUserAndRestaurantAndDate(user, restaurant, startOfDay, endOfDay)) {
            throw new IllegalStateException("오늘 이미 방문 기록을 등록했습니다.");
        }

        UserVisit visit = UserVisit.builder()
                .user(user)
                .restaurant(restaurant)
                .memo(request.getMemo())
                .visitedAt(now)
                .build();

        visitRepository.save(visit);
    }

    @Transactional(readOnly = true)
    public VisitListResponse getMyVisits(AppUser user) {
        List<UserVisit> visits = visitRepository.findRecentVisitsByUser(user);

        List<VisitResponse> responses = visits.stream()
                .map(v -> VisitResponse.builder()
                        .id(v.getId())
                        .restaurantId(v.getRestaurant().getId())
                        .restaurantName(v.getRestaurant().getName())
                        .memo(v.getMemo())
                        .visitedAt(v.getVisitedAt())
                        .build())
                .collect(Collectors.toList());

        return VisitListResponse.builder()
                .visits(responses)
                .totalCount(responses.size())
                .build();
    }

    // ===== 데이터 동기화 =====

    @Transactional
    public void syncRestaurantData() {
        log.info("맛집 데이터 동기화 시작 (반경 분할 전략: {})", Arrays.toString(RADIUS_RANGES));

        Set<String> syncedIds = new HashSet<>();

        // 음식점 (FD6) 동기화 - 반경별로 수집
        syncedIds.addAll(syncCategoryDataMultiRadius("FD6"));

        // 카페 (CE7) 동기화 - 반경별로 수집
        syncedIds.addAll(syncCategoryDataMultiRadius("CE7"));

        // 폐업 식당 비활성화
        int deactivatedCount = deactivateNotSyncedRestaurants(syncedIds);

        log.info("맛집 데이터 동기화 완료: 활성 {}개, 비활성화 {}개",
                syncedIds.size(), deactivatedCount);
    }

    /**
     * 반경 분할 전략으로 카테고리 데이터 동기화
     * - Kakao API의 pageable_count=45 제한을 우회
     * - 각 반경(200m, 400m, 700m, 1000m)별로 최대 45개씩 수집
     * - 중복 ID는 자동 제거 (Set 사용)
     */
    private Set<String> syncCategoryDataMultiRadius(String categoryCode) {
        Set<String> syncedIds = new HashSet<>();
        int totalSaved = 0;
        int totalSkipped = 0;

        for (int radius : RADIUS_RANGES) {
            int page = 1;
            boolean hasMore = true;
            int radiusSaved = 0;

            while (hasMore && page <= 3) {  // 각 반경당 최대 3페이지 (45개)
                KakaoPlaceResponse response = kakaoApiClient.searchByCategory(categoryCode, radius, page);

                if (response == null || response.getDocuments() == null || response.getDocuments().isEmpty()) {
                    break;
                }

                // 1. 페이지 내 모든 ID 수집
                List<String> placeIds = response.getDocuments().stream()
                        .map(KakaoPlace::getId)
                        .toList();

                // 2. 한 번의 쿼리로 기존 데이터 조회 (N+1 → 1)
                Map<String, Restaurant> existingMap = restaurantRepository.findAllById(placeIds)
                        .stream()
                        .collect(Collectors.toMap(Restaurant::getId, r -> r));

                // 3. 개별 처리
                for (KakaoPlace place : response.getDocuments()) {
                    String placeId = place.getId();

                    // 이미 처리된 ID는 스킵 (작은 반경에서 이미 수집됨)
                    if (syncedIds.contains(placeId)) {
                        totalSkipped++;
                        continue;
                    }

                    syncedIds.add(placeId);

                    Restaurant existing = existingMap.get(placeId);

                    if (existing == null) {
                        // 신규 식당
                        restaurantRepository.save(buildRestaurant(place));
                        radiusSaved++;
                        totalSaved++;
                    } else if (isDataChanged(existing, place)) {
                        // 기존 식당 - 데이터 변경됨
                        restaurantRepository.save(updateRestaurant(existing, place));
                        radiusSaved++;
                        totalSaved++;
                    }
                }

                hasMore = !response.getMeta().getIs_end();
                page++;
            }

            log.info("카테고리 {} 반경 {}m: 신규/수정 {}개, 누적 총 {}개",
                    categoryCode, radius, radiusSaved, syncedIds.size());
        }

        log.info("카테고리 {} 동기화 완료: 저장 {}개, 중복스킵 {}개, 총 {}개",
                categoryCode, totalSaved, totalSkipped, syncedIds.size());
        return syncedIds;
    }

    private boolean isDataChanged(Restaurant existing, KakaoPlace newData) {
        String newAddress = newData.getRoad_address_name() != null ?
                newData.getRoad_address_name() : newData.getAddress_name();

        return !Objects.equals(existing.getName(), newData.getPlace_name())
                || !Objects.equals(existing.getAddress(), newAddress)
                || !Objects.equals(existing.getPhone(), newData.getPhone())
                || !Objects.equals(existing.getCategory(), newData.getCategory_name());
    }

    private Restaurant buildRestaurant(KakaoPlace place) {
        String[] categoryLevels = parseCategoryLevels(place.getCategory_name());

        return Restaurant.builder()
                .id(place.getId())
                .name(place.getPlace_name())
                .category(place.getCategory_name())
                .categoryLevel1(categoryLevels[0])
                .categoryLevel2(categoryLevels[1])
                .categoryLevel3(categoryLevels[2])
                .address(place.getRoad_address_name() != null ?
                        place.getRoad_address_name() : place.getAddress_name())
                .phone(place.getPhone())
                .latitude(Double.parseDouble(place.getY()))
                .longitude(Double.parseDouble(place.getX()))
                .distance(Integer.parseInt(place.getDistance()))
                .placeUrl(place.getPlace_url())
                .isActive(true)
                .syncedAt(LocalDateTime.now())
                .build();
    }

    private Restaurant updateRestaurant(Restaurant existing, KakaoPlace place) {
        String[] categoryLevels = parseCategoryLevels(place.getCategory_name());

        existing.setName(place.getPlace_name());
        existing.setCategory(place.getCategory_name());
        existing.setCategoryLevel1(categoryLevels[0]);
        existing.setCategoryLevel2(categoryLevels[1]);
        existing.setCategoryLevel3(categoryLevels[2]);
        existing.setAddress(place.getRoad_address_name() != null ?
                place.getRoad_address_name() : place.getAddress_name());
        existing.setPhone(place.getPhone());
        existing.setLatitude(Double.parseDouble(place.getY()));
        existing.setLongitude(Double.parseDouble(place.getX()));
        existing.setDistance(Integer.parseInt(place.getDistance()));
        existing.setPlaceUrl(place.getPlace_url());
        existing.setIsActive(true);
        existing.setSyncedAt(LocalDateTime.now());
        return existing;
    }

    /**
     * 카테고리 문자열을 레벨별로 파싱
     * 예: "음식점 > 한식 > 육류,고기" → ["음식점", "한식", "육류,고기"]
     */
    private String[] parseCategoryLevels(String category) {
        String[] levels = new String[3];
        if (category == null || category.isEmpty()) {
            return levels;
        }

        String[] parts = category.split(" > ");
        for (int i = 0; i < Math.min(parts.length, 3); i++) {
            levels[i] = parts[i].trim();
        }
        return levels;
    }

    private int deactivateNotSyncedRestaurants(Set<String> syncedIds) {
        List<Restaurant> allActive = restaurantRepository.findByIsActiveTrue();
        int count = 0;

        for (Restaurant restaurant : allActive) {
            if (!syncedIds.contains(restaurant.getId())) {
                restaurant.setIsActive(false);
                restaurant.setSyncedAt(LocalDateTime.now());
                restaurantRepository.save(restaurant);
                log.info("식당 비활성화 (폐업 추정): {}", restaurant.getName());
                count++;
            }
        }

        return count;
    }

    // ===== 카테고리 마이그레이션 =====

    /**
     * 기존 데이터의 category 필드를 파싱하여 categoryLevel1~3 필드 채우기
     * 한 번만 실행하면 됨 (마이그레이션용)
     */
    @Transactional
    public int migrateCategoryLevels() {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        int count = 0;

        for (Restaurant r : restaurants) {
            if (r.getCategoryLevel1() == null && r.getCategory() != null) {
                String[] levels = parseCategoryLevels(r.getCategory());
                r.setCategoryLevel1(levels[0]);
                r.setCategoryLevel2(levels[1]);
                r.setCategoryLevel3(levels[2]);
                restaurantRepository.save(r);
                count++;
            }
        }

        log.info("카테고리 레벨 마이그레이션 완료: {}개 업데이트", count);
        return count;
    }

    // ===== 카테고리 분석 =====

    @Transactional(readOnly = true)
    public Map<String, Object> getCategoryAnalysis() {
        List<String> categories = restaurantRepository.findDistinctCategories();

        // 대분류별 중분류 및 소분류 집계
        Map<String, Map<String, Set<String>>> hierarchy = new LinkedHashMap<>();

        for (String category : categories) {
            if (category == null || category.isEmpty()) continue;

            String[] parts = category.split(" > ");
            if (parts.length < 1) continue;

            String level1 = parts[0].trim();
            String level2 = parts.length > 1 ? parts[1].trim() : null;
            String level3 = parts.length > 2 ? parts[2].trim() : null;

            hierarchy.computeIfAbsent(level1, k -> new LinkedHashMap<>());

            if (level2 != null) {
                hierarchy.get(level1).computeIfAbsent(level2, k -> new LinkedHashSet<>());
                if (level3 != null) {
                    hierarchy.get(level1).get(level2).add(level3);
                }
            }
        }

        // 응답 형태로 변환
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, Set<String>>> level1Entry : hierarchy.entrySet()) {
            Map<String, Object> level1Map = new LinkedHashMap<>();
            level1Map.put("name", level1Entry.getKey());

            List<Map<String, Object>> level2List = new ArrayList<>();
            for (Map.Entry<String, Set<String>> level2Entry : level1Entry.getValue().entrySet()) {
                Map<String, Object> level2Map = new LinkedHashMap<>();
                level2Map.put("name", level2Entry.getKey());
                level2Map.put("level3List", new ArrayList<>(level2Entry.getValue()));
                level2List.add(level2Map);
            }
            level1Map.put("level2List", level2List);
            result.add(level1Map);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("categories", result);
        response.put("rawCategories", categories);  // 디버깅용 원본 데이터
        return response;
    }

    // ===== 유틸리티 =====

    private String removeHtmlTags(String text) {
        if (text == null) return "";

        String result = text;

        // URL 디코딩 (% 인코딩 처리)
        try {
            if (result.contains("%")) {
                result = URLDecoder.decode(result, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            // 디코딩 실패 시 원본 유지
        }

        // HTML 태그 제거
        result = result.replaceAll("<[^>]*>", "");

        // HTML 엔티티 디코딩
        result = result.replace("&amp;", "&")
                       .replace("&lt;", "<")
                       .replace("&gt;", ">")
                       .replace("&quot;", "\"")
                       .replace("&#39;", "'")
                       .replace("&nbsp;", " ");
        return result;
    }

    private String formatDate(String date) {
        if (date == null || date.length() != 8) return date;
        return date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8);
    }
}
