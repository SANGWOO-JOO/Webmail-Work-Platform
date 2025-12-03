package dsn.webmail.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dsn.webmail.entity.MailKeyword;
import dsn.webmail.entity.ProcessedMail;
import dsn.webmail.entity.TechKeyword;
import dsn.webmail.entity.UserLearningHistory;
import dsn.webmail.repository.MailKeywordRepository;
import dsn.webmail.repository.TechKeywordRepository;
import dsn.webmail.repository.UserLearningHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailKeywordExtractorService {

    private final ChatLanguageModel chatModel;
    private final TechKeywordRepository keywordRepository;
    private final MailKeywordRepository mailKeywordRepository;
    private final UserLearningHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    /**
     * 메일에서 기술 키워드 추출 및 저장
     */
    @Transactional
    public List<TechKeyword> extractAndSaveKeywords(ProcessedMail mail) {
        try {
            // AI로 키워드 추출
            List<ExtractedKeyword> extracted = extractKeywordsFromMail(mail);

            if (extracted.isEmpty()) {
                log.debug("No keywords extracted from mail: {}", mail.getId());
                return Collections.emptyList();
            }

            // 추출된 키워드 저장
            List<TechKeyword> savedKeywords = extracted.stream()
                    .filter(ek -> ek.confidence() >= 0.7f) // 신뢰도 70% 이상만
                    .map(ek -> saveExtractedKeyword(mail, ek))
                    .toList();

            log.info("Extracted {} keywords from mail {}", savedKeywords.size(), mail.getId());
            return savedKeywords;

        } catch (Exception e) {
            log.error("Failed to extract keywords from mail {}: {}", mail.getId(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * AI를 사용하여 메일에서 기술 키워드 추출
     */
    private List<ExtractedKeyword> extractKeywordsFromMail(ProcessedMail mail) {
        String subject = mail.getSubject() != null ? mail.getSubject() : "";
        String body = mail.getContent() != null ? mail.getContent() : "";

        // 본문이 너무 길면 앞부분만 사용
        if (body.length() > 2000) {
            body = body.substring(0, 2000);
        }

        String prompt = """
                다음 이메일에서 기술/프로그래밍 관련 키워드를 추출해주세요.

                제목: %s
                본문: %s

                응답은 반드시 JSON 배열 형식으로만 응답하세요. 다른 텍스트 없이 JSON만 출력하세요.

                형식:
                [
                  {
                    "keyword": "Spring Boot IOC",
                    "category": "Backend",
                    "confidence": 0.95
                  }
                ]

                규칙:
                - 다음을 추출 대상으로 포함:
                  * 프로그래밍 언어 (Java, Python, JavaScript 등)
                  * 프레임워크/라이브러리 (Spring, React, Django 등)
                  * 도구 (Docker, Git, Kubernetes 등)
                  * CS 기본 개념 (프로세스, 스레드, 메모리, 알고리즘, 자료구조 등)
                  * 데이터베이스 (MySQL, Redis, MongoDB 등)
                  * 디자인 패턴 및 아키텍처 개념 (IOC, DI, MVC, REST, OOP 등)
                - 중요: 세부 개념이 있으면 "기술명 + 개념"으로 추출
                  * 예: "스프링부트 IOC / DI" → "Spring Boot IOC", "Spring Boot DI"
                  * 예: "JPA N+1 문제" → "JPA N+1"
                  * 예: "React Hook" → "React Hook"
                - 일반적인 단어(회의, 일정, 공부 등)는 제외
                - confidence는 해당 키워드가 기술 관련인지 확신도 (0.0~1.0)
                - category는 Backend, Frontend, Database, DevOps, CS기초, Other 중 하나
                - 기술 키워드가 없으면 빈 배열 [] 반환
                """.formatted(subject, body);

        try {
            String response = chatModel.generate(prompt);
            return parseKeywords(response);
        } catch (Exception e) {
            log.error("AI keyword extraction failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * AI 응답을 파싱하여 키워드 목록 추출
     */
    private List<ExtractedKeyword> parseKeywords(String response) {
        try {
            // JSON 부분만 추출
            String json = response.trim();
            if (json.contains("[")) {
                json = json.substring(json.indexOf("["), json.lastIndexOf("]") + 1);
            }

            return objectMapper.readValue(json, new TypeReference<List<ExtractedKeyword>>() {
            });
        } catch (Exception e) {
            log.error("Failed to parse keywords from response: {}", response);
            return Collections.emptyList();
        }
    }

    /**
     * 추출된 키워드를 DB에 저장
     */
    private TechKeyword saveExtractedKeyword(ProcessedMail mail, ExtractedKeyword ek) {
        // 키워드가 없으면 생성
        TechKeyword keyword = keywordRepository.findByKeyword(ek.keyword())
                .orElseGet(() -> createKeyword(ek));

        // 이미 매핑되어 있는지 확인
        if (!mailKeywordRepository.existsByMailAndKeyword(mail, keyword)) {
            // 메일-키워드 매핑 저장
            MailKeyword mailKeyword = MailKeyword.builder()
                    .mail(mail)
                    .keyword(keyword)
                    .confidence(ek.confidence())
                    .build();
            mailKeywordRepository.save(mailKeyword);

            // 글로벌 사용 횟수 증가
            keyword.incrementUsage();
            keywordRepository.save(keyword);

            // 사용자 학습 이력 업데이트
            updateUserHistory(mail.getUser().getId(), keyword);
        }

        return keyword;
    }

    /**
     * 새 키워드 생성
     */
    private TechKeyword createKeyword(ExtractedKeyword ek) {
        TechKeyword keyword = TechKeyword.builder()
                .keyword(ek.keyword())
                .category(ek.category())
                .description(generateDescription(ek.keyword()))
                .globalUsageCount(0)
                .build();
        return keywordRepository.save(keyword);
    }

    /**
     * 키워드 설명 생성
     */
    private String generateDescription(String keyword) {
        try {
            String prompt = "%s 기술을 한 문장으로 간단히 설명해주세요.".formatted(keyword);
            return chatModel.generate(prompt);
        } catch (Exception e) {
            return keyword + " 기술";
        }
    }

    /**
     * 사용자 학습 이력 업데이트
     */
    private void updateUserHistory(Long userId, TechKeyword keyword) {
        Optional<UserLearningHistory> existing = historyRepository
                .findByUserIdAndKeywordId(userId, keyword.getId());

        if (existing.isPresent()) {
            existing.get().incrementUsage();
        } else {
            // AppUser를 조회하지 않고 ID만 사용하기 위해 별도 처리 필요
            // 여기서는 기존 엔티티가 있을 때만 업데이트
        }
    }

    /**
     * 추출된 키워드 레코드
     */
    public record ExtractedKeyword(
            String keyword,
            String category,
            Float confidence) {
    }
}
