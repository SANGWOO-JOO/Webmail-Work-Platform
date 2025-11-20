package dsn.webmail.service;

import dsn.webmail.dto.ReplyDtos.ReplyDraftResponse;
import dsn.webmail.dto.ReplyDtos.ReplyGenerationRequest;
import dsn.webmail.entity.AppUser;
import dsn.webmail.entity.ProcessedMail;
import dsn.webmail.repository.AppUserRepository;
import dsn.webmail.repository.ProcessedMailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReplyGenerationService {

    private final ReplyGenerationAiService aiService;
    private final ProcessedMailRepository processedMailRepository;
    private final AppUserRepository appUserRepository;

    public ReplyDraftResponse generateReply(String email, Long mailId, ReplyGenerationRequest request) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        ProcessedMail mail = processedMailRepository.findByIdAndUserId(mailId, user.getId())
                .orElseThrow(() -> new RuntimeException("메일을 찾을 수 없습니다: " + mailId));

        log.debug("답장 초안 생성 시작: mailId={}, subject={}", mailId, mail.getSubject());

        // 톤/유형 기본값
        String tone = request.tone() != null ?
                request.tone().getDisplayName() : "중립적";
        String replyType = request.replyType() != null ?
                request.replyType().getDisplayName() : "확인";
        String context = request.additionalContext() != null && !request.additionalContext().isBlank() ?
                request.additionalContext() : "없음";

        // LLM 호출
        String draft = aiService.generateReply(
                mail.getSubject(),
                mail.getFromAddress(),
                truncateContent(mail.getContent(), 2000),
                tone,
                replyType,
                context
        );

        log.info("답장 초안 생성 완료: mailId={}, tone={}, replyType={}", mailId, tone, replyType);

        return new ReplyDraftResponse(
                mailId,
                draft,
                request.tone(),
                request.replyType(),
                LocalDateTime.now()
        );
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
