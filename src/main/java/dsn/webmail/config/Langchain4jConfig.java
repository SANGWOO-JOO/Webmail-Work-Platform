package dsn.webmail.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dsn.webmail.service.MailAnalysisAiService;
import dsn.webmail.service.ReplyGenerationAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Langchain4jConfig {

    @Value("${openai.api.key}")
    private String apiKey;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .temperature(0.3)
                .build();
    }

    @Bean
    public MailAnalysisAiService mailAnalysisAiService(ChatLanguageModel chatLanguageModel) {
        return AiServices.builder(MailAnalysisAiService.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
    }

    @Bean
    public ReplyGenerationAiService replyGenerationAiService(ChatLanguageModel chatLanguageModel) {
        return AiServices.builder(ReplyGenerationAiService.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
    }
}
