package dsn.webmail.controller;

import dsn.webmail.dto.MusicRecommendationResponse;
import dsn.webmail.service.MusicRecommendationService;
import dsn.webmail.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
public class MusicController {

    private final MusicRecommendationService musicRecommendationService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 음악 페이지
     */
    @GetMapping("/music")
    public String musicPage() {
        return "music/index";
    }

    /**
     * 프롬프트 기반 음악 추천
     */
    @PostMapping("/api/music/recommend")
    @ResponseBody
    public ResponseEntity<MusicRecommendationResponse> recommendByPrompt(
            @RequestBody Map<String, Object> request) {

        String prompt = (String) request.get("prompt");
        int limit = request.containsKey("limit") ? (int) request.get("limit") : 10;

        MusicRecommendationResponse response = musicRecommendationService.recommendByPrompt(prompt, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * 메일 기반 음악 추천
     */
    @GetMapping("/api/music/recommend/by-mail")
    @ResponseBody
    public ResponseEntity<MusicRecommendationResponse> recommendByMail(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "10") int limit) {

        String token = authHeader.replace("Bearer ", "");
        String email = jwtTokenProvider.getEmailFromToken(token);

        MusicRecommendationResponse response = musicRecommendationService.recommendByTodayMails(email, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * 필터 기반 음악 검색
     */
    @PostMapping("/api/music/search")
    @ResponseBody
    public ResponseEntity<MusicRecommendationResponse> searchByFilter(
            @RequestBody Map<String, Object> request) {

        String artist = (String) request.getOrDefault("artist", "");
        String genre = (String) request.getOrDefault("genre", "");
        String mood = (String) request.getOrDefault("mood", "");
        String country = (String) request.getOrDefault("country", "KR");
        int limit = request.containsKey("limit") ? (int) request.get("limit") : 10;

        MusicRecommendationResponse response = musicRecommendationService.searchByFilter(artist, genre, mood, country, limit);
        return ResponseEntity.ok(response);
    }
}
