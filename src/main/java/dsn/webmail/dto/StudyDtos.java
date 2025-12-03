package dsn.webmail.dto;

import dsn.webmail.entity.LearningResource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class StudyDtos {

    // 대시보드 응답
    public record DashboardResponse(
            SummaryStats summary,
            List<KeywordUsageResponse> recentKeywords,
            List<ResourceResponse> recommendedResources
    ) {}

    public record SummaryStats(
            Long totalKeywords,
            Long totalMails,
            Long bookmarkedCount,
            String topCategory
    ) {}

    // 메일 관련
    public record MailSummaryResponse(
            Long id,
            String subject,
            String fromAddress,
            String receivedAt,
            Float confidence
    ) {}

    public record KeywordMailsResponse(
            KeywordResponse keyword,
            List<MailSummaryResponse> mails
    ) {}

    // 키워드 관련
    public record KeywordResponse(
            Long id,
            String keyword,
            String category,
            String description,
            String iconClass,
            Long resourceCount,
            Integer usageCount
    ) {}

    public record KeywordListResponse(
            List<KeywordResponse> keywords
    ) {}

    public record KeywordUsageResponse(
            Long id,
            String keyword,
            String category,
            Long mailCount,
            LocalDateTime lastSeenAt
    ) {}

    // 학습 자료 관련
    public record ResourceResponse(
            Long id,
            String title,
            String url,
            LearningResource.ResourceType type,
            String source,
            String language,
            Integer difficulty,
            String summary,
            Boolean isBookmarked,
            String keyword
    ) {}

    public record KeywordResourcesResponse(
            KeywordResponse keyword,
            List<ResourceResponse> resources
    ) {}

    // 학습 이력 관련
    public record HistoryResponse(
            List<KeywordUsageResponse> history,
            HistoryStatistics statistics
    ) {}

    public record HistoryStatistics(
            Map<String, Long> byCategory,
            List<MonthlyCount> monthlyTrend
    ) {}

    public record MonthlyCount(
            String month,
            Long count
    ) {}

    // 북마크 관련
    public record BookmarkResponse(
            Long id,
            ResourceResponse resource,
            LocalDateTime createdAt
    ) {}

    public record BookmarkListResponse(
            List<BookmarkResponse> bookmarks
    ) {}

    public record BookmarkRequest(
            Long resourceId
    ) {}
}
