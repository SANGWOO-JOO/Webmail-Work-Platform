package dsn.webmail.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dsn.webmail.entity.MailEvent;
import dsn.webmail.repository.MailEventRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
// @AllArgsConstructor
public class EventExtractionService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final MailEventRepository repository;

    public EventExtractionService(MailEventRepository repository) {
        this.repository = repository;
    }

    public MailEvent extractEventFromMail(String mailContent) {
        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .temperature(0.3)
                .build();

        String prompt = createPrompt(mailContent);

        String aiResponse = model.generate(prompt);

        MailEvent event = parseAiResponse(aiResponse);

        MailEvent saved = repository.save(event);

        return saved;

    }

    /**
     * 프롬프트 생성 - 핵심!
     */
    private String createPrompt(String mailContent) {
        return String.format("""
            다음 이메일에서 회의/미팅 일정 정보를 추출해서 JSON 형식으로만 답변해줘.
            다른 설명은 절대 추가하지 말고, 오직 JSON만 출력해.
            
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
            - dateTime: "YYYY-MM-DD HH:mm" 형식으로 (오늘은 2025-11-19)
            - location: 장소 (없으면 "미정")
            - confidence: 추출 신뢰도 0.0~1.0 (확실하면 0.9 이상)
            
            JSON만 출력:
            """, mailContent);
    }


    /**
     * AI 응답 파싱
     */
    private MailEvent parseAiResponse(String aiResponse) {
        try {
            aiResponse = aiResponse.trim()
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(aiResponse);

            MailEvent event = new MailEvent().builder()
                    .title(json.get("title").asText())
                    .dateTime(json.get("dateTime").asText())
                    .location(json.get("location").asText())
                    .confidence((float) json.get("confidence").asDouble())
                    .build();

            return  event;
        } catch (Exception e) {
            MailEvent event = new MailEvent().builder()
                    .title("파싱 실패")
                    .dateTime("미정")
                    .location("미정")
                    .confidence(0.0f)
                    .build();

            return event;
        }
    }
}
