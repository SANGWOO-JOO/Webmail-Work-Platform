package dsn.webmail.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dsn.webmail.entity.LearningResource;
import dsn.webmail.entity.TechKeyword;
import dsn.webmail.repository.LearningResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LearningResourceRecommendationService {

    private final ChatLanguageModel chatModel;
    private final LearningResourceRepository resourceRepository;
    private final ObjectMapper objectMapper;

    /**
     * 키워드에 대한 학습 자료 조회 (없으면 AI 생성)
     */
    @Transactional
    public List<LearningResource> getOrGenerateResources(TechKeyword keyword) {
        List<LearningResource> existing = resourceRepository.findByKeywordId(keyword.getId());

        if (!existing.isEmpty()) {
            return existing;
        }

        // AI로 학습 자료 생성
        log.info("Generating learning resources for keyword: {}", keyword.getKeyword());
        List<LearningResource> generated = generateResources(keyword);

        if (!generated.isEmpty()) {
            resourceRepository.saveAll(generated);
            log.info("Generated {} resources for keyword: {}", generated.size(), keyword.getKeyword());
        }

        return generated;
    }

    /**
     * AI를 사용하여 학습 자료 생성
     */
    private List<LearningResource> generateResources(TechKeyword keyword) {
        String prompt = """
            '%s' 기술에 대한 학습 자료를 추천해주세요.

            응답은 반드시 JSON 배열 형식으로만 응답하세요. 다른 텍스트 없이 JSON만 출력하세요.

            형식:
            [
              {
                "title": "자료 제목",
                "url": "https://...",
                "type": "OFFICIAL_DOC",
                "source": "출처명",
                "language": "en",
                "summary": "한줄 요약"
              }
            ]

            규칙:
            - 공식 문서 1개 필수 (type: OFFICIAL_DOC)
              - 반드시 공식 사이트의 메인 문서 페이지 URL 사용
              - 예: https://spring.io/projects/spring-boot, https://docs.docker.com/
            - 신뢰할 수 있는 영어 튜토리얼 1-2개 (type: BLOG 또는 TUTORIAL)
              - 허용 출처: Baeldung (baeldung.com), DigitalOcean (digitalocean.com/community)
            - type은 OFFICIAL_DOC, BLOG, TUTORIAL, VIDEO 중 하나
            - 반드시 실제 존재하고 접속 가능한 URL만 제공
            - 블로그 개별 글이 아닌 메인 페이지나 카테고리 페이지 URL 사용
            - 총 2-3개의 자료만 추천 (품질 > 양)
            """.formatted(keyword.getKeyword());

        try {
            String response = chatModel.generate(prompt);
            List<GeneratedResource> generated = parseResources(response);

            // URL 검증 후 유효한 것만 저장
            List<LearningResource> validResources = new ArrayList<>();
            for (GeneratedResource gr : generated) {
                if (isUrlAccessible(gr.url())) {
                    validResources.add(toEntity(gr, keyword));
                } else {
                    log.warn("Invalid URL skipped: {}", gr.url());
                }
            }

            // 유효한 자료가 없으면 기본 공식 문서 URL 생성
            if (validResources.isEmpty()) {
                LearningResource fallback = createFallbackResource(keyword);
                if (fallback != null) {
                    validResources.add(fallback);
                }
            }

            return validResources;

        } catch (Exception e) {
            log.error("Failed to generate resources for {}: {}", keyword.getKeyword(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * URL 접속 가능 여부 확인
     */
    private boolean isUrlAccessible(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            // 2xx 또는 3xx 응답은 성공으로 간주
            return responseCode >= 200 && responseCode < 400;
        } catch (Exception e) {
            log.debug("URL not accessible: {} - {}", urlString, e.getMessage());
            return false;
        }
    }

    /**
     * 기본 검색 URL 생성 (fallback)
     */
    private LearningResource createFallbackResource(TechKeyword keyword) {
        // Google 검색 URL로 대체 (항상 접속 가능)
        String searchUrl = "https://www.google.com/search?q=" +
                keyword.getKeyword().replace(" ", "+") + "+documentation";

        return LearningResource.builder()
                .keyword(keyword)
                .title(keyword.getKeyword() + " 검색 결과")
                .url(searchUrl)
                .type(LearningResource.ResourceType.OFFICIAL_DOC)
                .source("Google")
                .language("en")
                .summary(keyword.getKeyword() + " 관련 문서 검색")
                .isRecommended(false)
                .build();
    }

    /**
     * AI 응답 파싱
     */
    private List<GeneratedResource> parseResources(String response) {
        try {
            String json = response.trim();
            if (json.contains("[")) {
                json = json.substring(json.indexOf("["), json.lastIndexOf("]") + 1);
            }

            return objectMapper.readValue(json, new TypeReference<List<GeneratedResource>>() {});
        } catch (Exception e) {
            log.error("Failed to parse resources: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * DTO를 엔티티로 변환
     */
    private LearningResource toEntity(GeneratedResource gr, TechKeyword keyword) {
        LearningResource.ResourceType type;
        try {
            type = LearningResource.ResourceType.valueOf(gr.type());
        } catch (Exception e) {
            type = LearningResource.ResourceType.BLOG;
        }

        return LearningResource.builder()
                .keyword(keyword)
                .title(gr.title())
                .url(gr.url())
                .type(type)
                .source(gr.source())
                .language(gr.language())
                .summary(gr.summary())
                .isRecommended(type == LearningResource.ResourceType.OFFICIAL_DOC)
                .build();
    }

    /**
     * 생성된 자료 레코드
     */
    public record GeneratedResource(
            String title,
            String url,
            String type,
            String source,
            String language,
            String summary
    ) {}
}
