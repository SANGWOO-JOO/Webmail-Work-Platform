package dsn.webmail.service;

import dsn.webmail.dto.StudyDtos.*;
import dsn.webmail.entity.*;
import dsn.webmail.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StudyService {

    private final AppUserRepository appUserRepository;
    private final TechKeywordRepository keywordRepository;
    private final LearningResourceRepository resourceRepository;
    private final UserResourceBookmarkRepository bookmarkRepository;
    private final MailKeywordRepository mailKeywordRepository;
    private final EventKeywordRepository eventKeywordRepository;
    private final LearningResourceRecommendationService resourceRecommendationService;

    /**
     * 학습 대시보드 데이터 조회
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String email) {
        AppUser user = getUser(email);

        // 메일 키워드 조회
        List<MailKeyword> userMailKeywords = mailKeywordRepository.findByMailUserIdOrderByCreatedAtDesc(user.getId());

        // 일정 키워드 조회
        List<EventKeyword> userEventKeywords = eventKeywordRepository
                .findByEventUserIdOrderByCreatedAtDesc(user.getId());

        // 모든 키워드 ID 수집
        Set<Long> uniqueKeywordIds = new HashSet<>();
        userMailKeywords.forEach(mk -> uniqueKeywordIds.add(mk.getKeyword().getId()));
        userEventKeywords.forEach(ek -> uniqueKeywordIds.add(ek.getKeyword().getId()));

        Long totalKeywords = (long) uniqueKeywordIds.size();

        // 관련 소스 수 (메일 + 일정)
        Long totalSources = userMailKeywords.stream()
                .map(mk -> mk.getMail().getId())
                .distinct()
                .count()
                + userEventKeywords.stream()
                        .map(ek -> ek.getEvent().getId())
                        .distinct()
                        .count();

        Long bookmarkedCount = bookmarkRepository.countByUserId(user.getId());
        String topCategory = getTopCategoryFromAll(userMailKeywords, userEventKeywords);

        SummaryStats summary = new SummaryStats(totalKeywords, totalSources, bookmarkedCount, topCategory);

        // 최근 키워드 (메일 + 일정에서 추출된)
        Map<Long, Object[]> latestByKeyword = new LinkedHashMap<>(); // [TechKeyword, LocalDateTime]

        // 메일 키워드 추가
        for (MailKeyword mk : userMailKeywords) {
            Long kwId = mk.getKeyword().getId();
            if (!latestByKeyword.containsKey(kwId)) {
                latestByKeyword.put(kwId, new Object[] { mk.getKeyword(), mk.getCreatedAt() });
            }
        }

        // 일정 키워드 추가 (더 최근이면 업데이트)
        for (EventKeyword ek : userEventKeywords) {
            Long kwId = ek.getKeyword().getId();
            if (!latestByKeyword.containsKey(kwId)) {
                latestByKeyword.put(kwId, new Object[] { ek.getKeyword(), ek.getCreatedAt() });
            } else {
                java.time.LocalDateTime existing = (java.time.LocalDateTime) latestByKeyword.get(kwId)[1];
                if (ek.getCreatedAt().isAfter(existing)) {
                    latestByKeyword.put(kwId, new Object[] { ek.getKeyword(), ek.getCreatedAt() });
                }
            }
        }

        List<KeywordUsageResponse> recentKeywords = latestByKeyword.values().stream()
                .limit(5)
                .map(arr -> {
                    TechKeyword kw = (TechKeyword) arr[0];
                    java.time.LocalDateTime lastSeen = (java.time.LocalDateTime) arr[1];
                    Long usageCount = mailKeywordRepository.countByKeywordId(kw.getId())
                            + eventKeywordRepository.countByKeywordId(kw.getId());
                    return new KeywordUsageResponse(
                            kw.getId(),
                            kw.getKeyword(),
                            kw.getCategory(),
                            usageCount,
                            lastSeen);
                })
                .toList();

        // 추천 자료 (최근 키워드 기반으로 동적 생성)
        List<ResourceResponse> recommendedResources = new ArrayList<>();
        for (KeywordUsageResponse kw : recentKeywords) {
            if (recommendedResources.size() >= 5)
                break;
            TechKeyword keyword = keywordRepository.findById(kw.id()).orElse(null);
            if (keyword != null) {
                List<LearningResource> resources = resourceRecommendationService.getOrGenerateResources(keyword);
                for (LearningResource r : resources) {
                    if (recommendedResources.size() >= 5)
                        break;
                    recommendedResources.add(toResourceResponse(r, user.getId()));
                }
            }
        }

        return new DashboardResponse(summary, recentKeywords, recommendedResources);
    }

    /**
     * 키워드 목록 조회 (사용자의 메일에서 추출된 키워드만)
     */
    @Transactional(readOnly = true)
    public KeywordListResponse getKeywords(String email, String category) {
        AppUser user = getUser(email);

        // 사용자 메일에서 추출된 키워드만 조회
        List<MailKeyword> userMailKeywords = mailKeywordRepository.findByMailUserIdOrderByCreatedAtDesc(user.getId());

        // 키워드별로 그룹핑
        Map<Long, List<MailKeyword>> byKeyword = userMailKeywords.stream()
                .collect(Collectors.groupingBy(mk -> mk.getKeyword().getId()));

        List<KeywordResponse> keywordResponses = byKeyword.entrySet().stream()
                .map(entry -> {
                    TechKeyword k = entry.getValue().get(0).getKeyword();

                    // 카테고리 필터
                    if (category != null && !category.isBlank() && !k.getCategory().equals(category)) {
                        return null;
                    }

                    Long resourceCount = resourceRepository.countByKeywordId(k.getId());
                    Integer usageCount = entry.getValue().size(); // 해당 키워드가 포함된 메일 수

                    return new KeywordResponse(
                            k.getId(),
                            k.getKeyword(),
                            k.getCategory(),
                            k.getDescription(),
                            k.getIconClass(),
                            resourceCount,
                            usageCount);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(KeywordResponse::keyword))
                .toList();

        return new KeywordListResponse(keywordResponses);
    }

    /**
     * 키워드 관련 메일 조회
     */
    @Transactional(readOnly = true)
    public KeywordMailsResponse getKeywordMails(String email, Long keywordId) {
        AppUser user = getUser(email);

        TechKeyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new RuntimeException("키워드를 찾을 수 없습니다: " + keywordId));

        // 해당 키워드가 포함된 메일 조회
        List<MailKeyword> mailKeywords = mailKeywordRepository.findByKeywordIdOrderByCreatedAtDesc(keywordId);

        // 사용자 메일만 필터
        List<MailSummaryResponse> mails = mailKeywords.stream()
                .filter(mk -> mk.getMail().getUser().getId().equals(user.getId()))
                .map(mk -> {
                    ProcessedMail mail = mk.getMail();
                    return new MailSummaryResponse(
                            mail.getId(),
                            mail.getSubject(),
                            mail.getFromAddress(),
                            mail.getProcessedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                            mk.getConfidence());
                })
                .toList();

        Long resourceCount = resourceRepository.countByKeywordId(keywordId);
        KeywordResponse keywordResponse = new KeywordResponse(
                keyword.getId(),
                keyword.getKeyword(),
                keyword.getCategory(),
                keyword.getDescription(),
                keyword.getIconClass(),
                resourceCount,
                mails.size());

        return new KeywordMailsResponse(keywordResponse, mails);
    }

    /**
     * 키워드별 학습 자료 조회 (동적 생성)
     */
    @Transactional
    public KeywordResourcesResponse getKeywordResources(String email, Long keywordId,
            LearningResource.ResourceType type, Integer difficulty) {
        AppUser user = getUser(email);

        TechKeyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new RuntimeException("키워드를 찾을 수 없습니다: " + keywordId));

        // 동적으로 학습 자료 생성/조회
        List<LearningResource> resources = resourceRecommendationService.getOrGenerateResources(keyword);

        // 필터 적용
        if (type != null) {
            resources = resources.stream()
                    .filter(r -> r.getType() == type)
                    .toList();
        }
        if (difficulty != null) {
            resources = resources.stream()
                    .filter(r -> Objects.equals(r.getDifficulty(), difficulty))
                    .toList();
        }

        Long resourceCount = (long) resources.size();
        Integer usageCount = mailKeywordRepository.countByKeywordId(keywordId).intValue();

        KeywordResponse keywordResponse = new KeywordResponse(
                keyword.getId(),
                keyword.getKeyword(),
                keyword.getCategory(),
                keyword.getDescription(),
                keyword.getIconClass(),
                resourceCount,
                usageCount);

        List<ResourceResponse> resourceResponses = resources.stream()
                .map(r -> toResourceResponse(r, user.getId()))
                .toList();

        return new KeywordResourcesResponse(keywordResponse, resourceResponses);
    }

    /**
     * 학습 이력 조회 (메일 기반)
     */
    @Transactional(readOnly = true)
    public HistoryResponse getHistory(String email) {
        AppUser user = getUser(email);

        // 사용자 메일에서 추출된 키워드 기반 이력
        List<MailKeyword> userMailKeywords = mailKeywordRepository.findByMailUserIdOrderByCreatedAtDesc(user.getId());

        // 키워드별로 그룹핑
        Map<Long, List<MailKeyword>> byKeyword = userMailKeywords.stream()
                .collect(Collectors.groupingBy(mk -> mk.getKeyword().getId()));

        List<KeywordUsageResponse> history = byKeyword.entrySet().stream()
                .map(entry -> {
                    TechKeyword k = entry.getValue().get(0).getKeyword();
                    Long mailCount = (long) entry.getValue().size();
                    // 가장 최근 메일의 날짜
                    var lastSeen = entry.getValue().stream()
                            .max(Comparator.comparing(MailKeyword::getCreatedAt))
                            .map(MailKeyword::getCreatedAt)
                            .orElse(null);
                    return new KeywordUsageResponse(
                            k.getId(),
                            k.getKeyword(),
                            k.getCategory(),
                            mailCount,
                            lastSeen);
                })
                .sorted((a, b) -> Long.compare(b.mailCount(), a.mailCount())) // 메일 수 기준 정렬
                .toList();

        // 카테고리별 통계
        Map<String, Long> byCategory = userMailKeywords.stream()
                .collect(Collectors.groupingBy(
                        mk -> mk.getKeyword().getCategory(),
                        Collectors.counting()));

        // 월별 추이 (간단 구현)
        List<MonthlyCount> monthlyTrend = Collections.emptyList();

        HistoryStatistics statistics = new HistoryStatistics(byCategory, monthlyTrend);

        return new HistoryResponse(history, statistics);
    }

    /**
     * 북마크 추가
     */
    @Transactional
    public void addBookmark(String email, Long resourceId) {
        AppUser user = getUser(email);
        LearningResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("자료를 찾을 수 없습니다: " + resourceId));

        if (bookmarkRepository.existsByUserIdAndResourceId(user.getId(), resourceId)) {
            throw new RuntimeException("이미 북마크된 자료입니다.");
        }

        UserResourceBookmark bookmark = UserResourceBookmark.builder()
                .user(user)
                .resource(resource)
                .build();
        bookmarkRepository.save(bookmark);
        log.info("북마크 추가: userId={}, resourceId={}", user.getId(), resourceId);
    }

    /**
     * 북마크 삭제
     */
    @Transactional
    public void removeBookmark(String email, Long resourceId) {
        AppUser user = getUser(email);
        bookmarkRepository.deleteByUserIdAndResourceId(user.getId(), resourceId);
        log.info("북마크 삭제: userId={}, resourceId={}", user.getId(), resourceId);
    }

    /**
     * 북마크 목록 조회
     */
    @Transactional(readOnly = true)
    public BookmarkListResponse getBookmarks(String email) {
        AppUser user = getUser(email);

        List<UserResourceBookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<BookmarkResponse> bookmarkResponses = bookmarks.stream()
                .map(b -> new BookmarkResponse(
                        b.getId(),
                        toResourceResponse(b.getResource(), user.getId()),
                        b.getCreatedAt()))
                .toList();

        return new BookmarkListResponse(bookmarkResponses);
    }

    private AppUser getUser(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));
    }

    private String getTopCategoryFromAll(List<MailKeyword> mailKeywords, List<EventKeyword> eventKeywords) {
        Map<String, Long> categoryCount = new HashMap<>();

        // 메일 키워드 카운트
        mailKeywords.forEach(mk -> {
            String cat = mk.getKeyword().getCategory();
            categoryCount.put(cat, categoryCount.getOrDefault(cat, 0L) + 1);
        });

        // 일정 키워드 카운트
        eventKeywords.forEach(ek -> {
            String cat = ek.getKeyword().getCategory();
            categoryCount.put(cat, categoryCount.getOrDefault(cat, 0L) + 1);
        });

        return categoryCount.entrySet().stream()
                .max(Comparator.comparing(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("없음");
    }

    private ResourceResponse toResourceResponse(LearningResource resource, Long userId) {
        boolean isBookmarked = bookmarkRepository.existsByUserIdAndResourceId(userId, resource.getId());
        return new ResourceResponse(
                resource.getId(),
                resource.getTitle(),
                resource.getUrl(),
                resource.getType(),
                resource.getSource(),
                resource.getLanguage(),
                resource.getDifficulty(),
                resource.getSummary(),
                isBookmarked,
                resource.getKeyword().getKeyword());
    }
}
