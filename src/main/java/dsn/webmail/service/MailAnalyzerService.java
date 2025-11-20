package dsn.webmail.service;

import dsn.webmail.dto.MailAnalysisResult;
import dsn.webmail.entity.MailCategory;
import dsn.webmail.entity.ProcessedMail;
import dsn.webmail.repository.ProcessedMailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailAnalyzerService {

    private final MailAnalysisAiService mailAnalysisAiService;
    private final ProcessedMailRepository processedMailRepository;

    /**
     * 메일 분석 (비동기)
     */
    @Async
    @Transactional
    public void analyzeMailAsync(Long mailId) {
        try {
            ProcessedMail mail = processedMailRepository.findById(mailId)
                    .orElseThrow(() -> new RuntimeException("메일을 찾을 수 없습니다: " + mailId));

            analyzeMail(mail);
        } catch (Exception e) {
            log.error("메일 분석 실패: mailId={}, error={}", mailId, e.getMessage());
        }
    }

    /**
     * 메일 분석 (동기)
     */
    @Transactional
    public void analyzeMail(ProcessedMail mail) {
        try {
            log.debug("메일 분석 시작: id={}, subject={}", mail.getId(), mail.getSubject());

            // 내용이 너무 길면 자르기 (토큰 제한)
            String content = truncateContent(mail.getContent(), 2000);

            // LLM 분석 호출
            MailAnalysisResult result = mailAnalysisAiService.analyze(
                    mail.getSubject(),
                    mail.getFromAddress(),
                    content
            );

            // 결과 저장
            mail.setCategory(result.category());
            mail.setCategoryConfidence(result.confidence());
            mail.setSummary(result.summary());
            mail.setAnalyzedAt(LocalDateTime.now());

            processedMailRepository.save(mail);

            log.info("메일 분석 완료: id={}, category={}, confidence={}",
                    mail.getId(), result.category(), result.confidence());

        } catch (Exception e) {
            log.error("메일 분석 오류: id={}, error={}", mail.getId(), e.getMessage());

            // 분석 실패 시 UNKNOWN으로 설정
            mail.setCategory(MailCategory.UNKNOWN);
            mail.setAnalyzedAt(LocalDateTime.now());
            processedMailRepository.save(mail);
        }
    }

    /**
     * 메일 재분석
     */
    @Transactional
    public ProcessedMail reanalyzeMail(Long mailId) {
        ProcessedMail mail = processedMailRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("메일을 찾을 수 없습니다: " + mailId));

        analyzeMail(mail);
        return mail;
    }

    /**
     * 내용 길이 제한
     */
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
