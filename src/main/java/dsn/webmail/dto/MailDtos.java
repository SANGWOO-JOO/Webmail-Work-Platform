package dsn.webmail.dto;

import dsn.webmail.entity.MailCategory;

import java.time.LocalDateTime;
import java.util.List;

public class MailDtos {

    public record MailListItemResponse(
            Long id,
            String messageId,
            String subject,
            String fromAddress,
            LocalDateTime processedAt,
            MailCategory category,
            String categoryDisplayName,
            Float categoryConfidence,
            String summary
    ) {}

    public record MailListResponse(
            List<MailListItemResponse> mails,
            long totalCount,
            int page,
            int size
    ) {}

    public record MailDetailResponse(
            Long id,
            String messageId,
            String subject,
            String fromAddress,
            String content,
            LocalDateTime processedAt,
            MailCategory category,
            String categoryDisplayName,
            Float categoryConfidence,
            String summary,
            LocalDateTime analyzedAt
    ) {}

    // 메일 답장 요청
    public record MailReplyRequest(
            String to,
            String subject,
            String body,
            Long originalMailId
    ) {}

    // 메일 답장 응답
    public record MailReplyResponse(
            boolean success,
            String message
    ) {}
}
