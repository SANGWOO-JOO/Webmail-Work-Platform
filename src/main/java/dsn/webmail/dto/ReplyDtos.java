package dsn.webmail.dto;

import dsn.webmail.entity.ReplyTone;
import dsn.webmail.entity.ReplyType;

import java.time.LocalDateTime;

public class ReplyDtos {

    public record ReplyGenerationRequest(
            ReplyTone tone,
            ReplyType replyType,
            String additionalContext
    ) {}

    public record ReplyDraftResponse(
            Long mailId,
            String replyDraft,
            ReplyTone tone,
            ReplyType replyType,
            LocalDateTime generatedAt
    ) {}
}
