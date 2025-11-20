package dsn.webmail.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dsn.webmail.entity.AppUser;
import dsn.webmail.entity.MailEvent;
import dsn.webmail.repository.MailEventRepository;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class EventExtractionService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final MailEventRepository repository;

    public EventExtractionService(MailEventRepository repository) {
        this.repository = repository;
    }

    public MailEvent extractEventFromMail(AppUser user, String messageId, String mailContent) {
        try {
            ChatLanguageModel model = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName("gpt-4o-mini")
                    .temperature(0.3)
                    .build();

            String prompt = createPrompt(mailContent);

            String aiResponse = model.generate(prompt);

            MailEvent event = parseAiResponse(aiResponse, user, messageId);

            if (event != null && event.getConfidence() != null && event.getConfidence() > 0) {
                MailEvent saved = repository.save(event);
                log.info("Event extracted from mail {}: {} (confidence: {})",
                        messageId, event.getTitle(), event.getConfidence());
                return saved;
            }

            if (event != null) {
                log.debug("Event skipped due to low confidence: {} (confidence: {})",
                        messageId, event.getConfidence());
            }

            return null;

        } catch (Exception e) {
            log.error("Failed to extract event from mail {}: {}", messageId, e.getMessage());
            return null;
        }
    }

    /**
     * 프롬프트 생성 - 핵심!
     */
    private String createPrompt(String mailContent) {
        LocalDateTime now = LocalDateTime.now();
        String currentDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        String tomorrowDate = now.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String dayAfterTomorrowDate = now.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return String.format("""
            다음 이메일에서 회의/미팅 일정 정보를 추출해서 JSON 형식으로만 답변해줘.
            다른 설명은 절대 추가하지 말고, 오직 JSON만 출력해.

            # 현재 시각 정보:
            - 현재 날짜: %s
            - 현재 시간: %s
            - 내일 날짜: %s
            - 모레 날짜: %s

            # 이메일 내용:
            %s

            # 출력 형식 (이 형식을 정확히 지켜줘):
            {
              "title": "회의 제목",
              "dateTime": "2025-11-26 14:00",
              "location": "3층 회의실A",
              "confidence": 0.95
            }

            # 규칙:
            - title: 회의/미팅 제목
            - dateTime: "YYYY-MM-DD HH:mm" 형식으로 반드시 변환
            - location: 장소 (없으면 "미정")
            - confidence: 추출 신뢰도 0.0~1.0 (확실하면 0.9 이상)

            # 상대적 날짜 표현 변환 규칙:
            - "오늘", "금일", "당일" → %s
            - "내일", "명일", "익일" → %s
            - "모레", "내일모레" → %s
            - "이번 주 월요일/화요일..." → 해당 요일의 실제 날짜로 변환
            - "다음 주 월요일/화요일..." → 다음 주 해당 요일의 실제 날짜로 변환
            - 시간이 명시되지 않은 경우 "09:00"을 기본값으로 사용

            JSON만 출력:
            """, currentDate, currentTime, tomorrowDate, dayAfterTomorrowDate,
                 mailContent, currentDate, tomorrowDate, dayAfterTomorrowDate);
    }


    /**
     * AI 응답 파싱
     */
    private MailEvent parseAiResponse(String aiResponse, AppUser user, String messageId) {
        try {
            aiResponse = aiResponse.trim()
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(aiResponse);

            MailEvent event = MailEvent.builder()
                    .user(user)
                    .sourceMessageId(messageId)
                    .title(json.get("title").asText())
                    .dateTime(json.get("dateTime").asText())
                    .location(json.get("location").asText())
                    .confidence((float) json.get("confidence").asDouble())
                    .build();

            return event;
        } catch (Exception e) {
            log.warn("Failed to parse AI response: {}", e.getMessage());
            return null;
        }
    }
}
