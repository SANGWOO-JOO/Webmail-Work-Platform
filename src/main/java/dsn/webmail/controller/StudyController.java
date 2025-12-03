package dsn.webmail.controller;

import dsn.webmail.dto.StudyDtos.*;
import dsn.webmail.entity.LearningResource;
import dsn.webmail.service.StudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * 학습 Controller
 * - View: Thymeleaf 템플릿 반환
 * - API: JSON 반환 (REST API)
 */
@Controller
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    // ========== View 엔드포인트 ==========

    /**
     * 학습 메인 페이지
     */
    @GetMapping
    public String study() {
        return "study/index";
    }

    // ========== API 엔드포인트 ==========

    /**
     * 학습 대시보드 조회
     */
    @GetMapping("/api/dashboard")
    @ResponseBody
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal String email) {
        DashboardResponse dashboard = studyService.getDashboard(email);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * 키워드 목록 조회
     */
    @GetMapping("/api/keywords")
    @ResponseBody
    public ResponseEntity<KeywordListResponse> getKeywords(
            @AuthenticationPrincipal String email,
            @RequestParam(required = false) String category) {
        KeywordListResponse keywords = studyService.getKeywords(email, category);
        return ResponseEntity.ok(keywords);
    }

    /**
     * 키워드별 학습 자료 조회
     */
    @GetMapping("/api/keywords/{keywordId}/resources")
    @ResponseBody
    public ResponseEntity<KeywordResourcesResponse> getKeywordResources(
            @AuthenticationPrincipal String email,
            @PathVariable Long keywordId,
            @RequestParam(required = false) LearningResource.ResourceType type,
            @RequestParam(required = false) Integer difficulty) {
        KeywordResourcesResponse resources = studyService.getKeywordResources(email, keywordId, type, difficulty);
        return ResponseEntity.ok(resources);
    }

    /**
     * 키워드 관련 메일 조회
     */
    @GetMapping("/api/keywords/{keywordId}/mails")
    @ResponseBody
    public ResponseEntity<KeywordMailsResponse> getKeywordMails(
            @AuthenticationPrincipal String email,
            @PathVariable Long keywordId) {
        KeywordMailsResponse mails = studyService.getKeywordMails(email, keywordId);
        return ResponseEntity.ok(mails);
    }

    /**
     * 학습 이력 조회
     */
    @GetMapping("/api/history")
    @ResponseBody
    public ResponseEntity<HistoryResponse> getHistory(
            @AuthenticationPrincipal String email) {
        HistoryResponse history = studyService.getHistory(email);
        return ResponseEntity.ok(history);
    }

    /**
     * 북마크 추가
     */
    @PostMapping("/api/bookmarks")
    @ResponseBody
    public ResponseEntity<Void> addBookmark(
            @AuthenticationPrincipal String email,
            @RequestBody BookmarkRequest request) {
        studyService.addBookmark(email, request.resourceId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 북마크 삭제
     */
    @DeleteMapping("/api/bookmarks/{resourceId}")
    @ResponseBody
    public ResponseEntity<Void> removeBookmark(
            @AuthenticationPrincipal String email,
            @PathVariable Long resourceId) {
        studyService.removeBookmark(email, resourceId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 북마크 목록 조회
     */
    @GetMapping("/api/bookmarks")
    @ResponseBody
    public ResponseEntity<BookmarkListResponse> getBookmarks(
            @AuthenticationPrincipal String email) {
        BookmarkListResponse bookmarks = studyService.getBookmarks(email);
        return ResponseEntity.ok(bookmarks);
    }
}
