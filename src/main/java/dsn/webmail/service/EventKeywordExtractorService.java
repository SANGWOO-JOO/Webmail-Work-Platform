package dsn.webmail.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dsn.webmail.entity.EventKeyword;
import dsn.webmail.entity.MailEvent;
import dsn.webmail.entity.TechKeyword;
import dsn.webmail.repository.EventKeywordRepository;
import dsn.webmail.repository.TechKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventKeywordExtractorService {

    private final ChatLanguageModel chatModel;
    private final TechKeywordRepository keywordRepository;
    private final EventKeywordRepository eventKeywordRepository;
    private final ObjectMapper objectMapper;

    /**
     * 일정에서 기술 키워드 추출 및 저장
     */
    @Transactional
    public List<TechKeyword> extractAndSaveKeywords(MailEvent event) {
        try {
            // 기존 키워드 삭제 (수정 시)
            eventKeywordRepository.deleteByEvent(event);

            // AI로 키워드 추출
            String text = buildTextForExtraction(event);
            List<ExtractedKeyword> extracted = extractKeywordsFromText(text);

            if (extracted.isEmpty()) {
                log.debug("No keywords extracted from event: {}", event.getId());
                return Collections.emptyList();
            }

            // 추출된 키워드 저장
            List<TechKeyword> savedKeywords = extracted.stream()
                    .filter(ek -> ek.confidence() >= 0.7f)
                    .map(ek -> saveExtractedKeyword(event, ek))
                    .toList();

            log.info("Extracted {} keywords from event {}", savedKeywords.size(), event.getId());
            return savedKeywords;

        } catch (Exception e) {
            log.error("Failed to extract keywords from event {}: {}", event.getId(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private String buildTextForExtraction(MailEvent event) {
        StringBuilder sb = new StringBuilder();
        if (event.getTitle() != null) {
            sb.append(event.getTitle()).append(" ");
        }
        if (event.getDescription() != null) {
            sb.append(event.getDescription());
        }
        return sb.toString().trim();
    }

    private List<ExtractedKeyword> extractKeywordsFromText(String text) {
        if (text.isBlank()) {
            return Collections.emptyList();
        }

        // 텍스트가 너무 길면 자르기
        if (text.length() > 1000) {
            text = text.substring(0, 1000);
        }

        String prompt = """
            다음 일정 정보에서 기술/프로그래밍 관련 키워드를 추출해주세요.

            내용: %s

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
            """.formatted(text);

        try {
            String response = chatModel.generate(prompt);
            return parseKeywords(response);
        } catch (Exception e) {
            log.error("AI keyword extraction failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<ExtractedKeyword> parseKeywords(String response) {
        try {
            String json = response.trim();
            if (json.contains("[")) {
                json = json.substring(json.indexOf("["), json.lastIndexOf("]") + 1);
            }
            return objectMapper.readValue(json, new TypeReference<List<ExtractedKeyword>>() {});
        } catch (Exception e) {
            log.error("Failed to parse keywords from response: {}", response);
            return Collections.emptyList();
        }
    }

    private TechKeyword saveExtractedKeyword(MailEvent event, ExtractedKeyword ek) {
        // 키워드가 없으면 생성
        TechKeyword keyword = keywordRepository.findByKeyword(ek.keyword())
                .orElseGet(() -> createKeyword(ek));

        // 이벤트-키워드 매핑 저장
        if (!eventKeywordRepository.existsByEventAndKeyword(event, keyword)) {
            EventKeyword eventKeyword = EventKeyword.builder()
                    .event(event)
                    .keyword(keyword)
                    .build();
            eventKeywordRepository.save(eventKeyword);

            // 글로벌 사용 횟수 증가
            keyword.incrementUsage();
            keywordRepository.save(keyword);
        }

        return keyword;
    }

    private TechKeyword createKeyword(ExtractedKeyword ek) {
        TechKeyword keyword = TechKeyword.builder()
                .keyword(ek.keyword())
                .category(ek.category())
                .description(ek.keyword() + " 기술")
                .globalUsageCount(0)
                .build();
        return keywordRepository.save(keyword);
    }

    public record ExtractedKeyword(
            String keyword,
            String category,
            Float confidence
    ) {}
}
