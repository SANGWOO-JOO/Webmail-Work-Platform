package dsn.webmail.controller;

import dsn.webmail.dto.MapDtos.*;
import dsn.webmail.entity.AppUser;
import dsn.webmail.repository.AppUserRepository;
import dsn.webmail.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;
    private final AppUserRepository userRepository;

    // ===== 페이지 =====

    @GetMapping("/map")
    public String mapPage() {
        return "map/index";
    }

    // ===== API =====

    @GetMapping("/map/api/restaurants")
    @ResponseBody
    public ResponseEntity<RestaurantListResponse> getRestaurants(
            @AuthenticationPrincipal String email,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String categoryLevel1,
            @RequestParam(required = false) String categoryLevel2,
            @RequestParam(required = false) String categoryLevel3,
            @RequestParam(defaultValue = "500") Integer radius,
            @RequestParam(defaultValue = "distance") String sort,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "false") Boolean favoritesOnly) {

        AppUser user = getUserByEmail(email);
        RestaurantSearchRequest request = new RestaurantSearchRequest();
        request.setCategory(category);
        request.setCategoryLevel1(categoryLevel1);
        request.setCategoryLevel2(categoryLevel2);
        request.setCategoryLevel3(categoryLevel3);
        request.setRadius(radius);
        request.setSort(sort);
        request.setPage(page);
        request.setFavoritesOnly(favoritesOnly);

        return ResponseEntity.ok(mapService.getRestaurants(user, request));
    }

    @GetMapping("/map/api/restaurants/{id}/detail")
    @ResponseBody
    public ResponseEntity<RestaurantDetailResponse> getRestaurantDetail(
            @PathVariable String id) {
        return ResponseEntity.ok(mapService.getRestaurantDetail(id));
    }

    @GetMapping("/map/api/restaurants/{id}/social")
    @ResponseBody
    public ResponseEntity<SocialInfoResponse> getSocialInfo(
            @AuthenticationPrincipal String email,
            @PathVariable String id) {
        AppUser user = getUserByEmail(email);
        return ResponseEntity.ok(mapService.getSocialInfo(user, id));
    }

    // ===== 즐겨찾기 =====

    @GetMapping("/map/api/favorites")
    @ResponseBody
    public ResponseEntity<FavoriteListResponse> getFavorites(
            @AuthenticationPrincipal String email) {
        AppUser user = getUserByEmail(email);
        return ResponseEntity.ok(mapService.getFavorites(user));
    }

    @PostMapping("/map/api/favorites")
    @ResponseBody
    public ResponseEntity<Void> addFavorite(
            @AuthenticationPrincipal String email,
            @RequestBody FavoriteRequest request) {
        AppUser user = getUserByEmail(email);
        mapService.addFavorite(user, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/map/api/favorites/{restaurantId}")
    @ResponseBody
    public ResponseEntity<Void> removeFavorite(
            @AuthenticationPrincipal String email,
            @PathVariable String restaurantId) {
        AppUser user = getUserByEmail(email);
        mapService.removeFavorite(user, restaurantId);
        return ResponseEntity.ok().build();
    }

    // ===== 좋아요/싫어요 =====

    @PostMapping("/map/api/restaurants/{id}/reaction")
    @ResponseBody
    public ResponseEntity<ReactionResponse> addReaction(
            @AuthenticationPrincipal String email,
            @PathVariable String id,
            @RequestBody ReactionRequest request) {
        AppUser user = getUserByEmail(email);
        ReactionResponse response = mapService.addReaction(user, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/map/api/restaurants/{id}/reaction")
    @ResponseBody
    public ResponseEntity<Void> removeReaction(
            @AuthenticationPrincipal String email,
            @PathVariable String id) {
        AppUser user = getUserByEmail(email);
        mapService.removeReaction(user, id);
        return ResponseEntity.ok().build();
    }

    // ===== 방문 기록 =====

    @PostMapping("/map/api/restaurants/{id}/visit")
    @ResponseBody
    public ResponseEntity<Void> addVisit(
            @AuthenticationPrincipal String email,
            @PathVariable String id,
            @RequestBody VisitRequest request) {
        AppUser user = getUserByEmail(email);
        mapService.addVisit(user, id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/map/api/my-visits")
    @ResponseBody
    public ResponseEntity<VisitListResponse> getMyVisits(
            @AuthenticationPrincipal String email) {
        AppUser user = getUserByEmail(email);
        return ResponseEntity.ok(mapService.getMyVisits(user));
    }

    // ===== 카테고리 =====

    @GetMapping("/map/api/categories")
    @ResponseBody
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(mapService.getCategoryAnalysis());
    }

    // ===== 관리자 기능 =====

    @PostMapping("/map/api/sync")
    @ResponseBody
    public ResponseEntity<String> syncRestaurantData() {
        mapService.syncRestaurantData();
        return ResponseEntity.ok("동기화 완료");
    }

    @PostMapping("/map/api/migrate-categories")
    @ResponseBody
    public ResponseEntity<String> migrateCategoryLevels() {
        int count = mapService.migrateCategoryLevels();
        return ResponseEntity.ok("카테고리 마이그레이션 완료: " + count + "개 업데이트");
    }

    // ===== 유틸리티 =====

    private AppUser getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }
}
