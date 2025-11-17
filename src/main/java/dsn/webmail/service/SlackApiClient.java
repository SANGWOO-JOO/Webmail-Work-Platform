package dsn.webmail.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class SlackApiClient {
    
    private final RestTemplate restTemplate;
    private final String botToken;

    public SlackApiClient(@Value("${slack.bot.token}") String botToken) {
        this.restTemplate = new RestTemplate();
        this.botToken = botToken;
    }

    public String lookupUserIdByEmail(String email) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(botToken);
            
            String url = "https://slack.com/api/users.lookupByEmail?email=" + email;
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<SlackUserLookupResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, request, SlackUserLookupResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                SlackUserLookupResponse body = response.getBody();
                if (body.ok() && body.user() != null) {
                    log.debug("Found Slack user ID {} for email {}", body.user().id(), email);
                    return body.user().id();
                } else {
                    log.warn("Slack API returned ok=false for email {}: {}", email, body.error());
                    return null;
                }
            } else {
                log.warn("Slack API returned non-2xx status for email {}: {}", email, response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            log.error("Failed to lookup Slack user ID for email {}: {}", email, e.getMessage());
            return null;
        }
    }

    public void sendDirectMessage(String slackUserId, String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(botToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            SlackChatMessage message = new SlackChatMessage(slackUserId, text);
            HttpEntity<SlackChatMessage> request = new HttpEntity<>(message, headers);
            
            ResponseEntity<SlackChatResponse> response = restTemplate.postForEntity(
                "https://slack.com/api/chat.postMessage", request, SlackChatResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                SlackChatResponse body = response.getBody();
                if (body.ok()) {
                    log.debug("Slack DM sent successfully to user {}", slackUserId);
                } else {
                    log.warn("Slack chat.postMessage returned ok=false for user {}: {}", slackUserId, body.error());
                    throw new RuntimeException("Slack 메시지 전송 실패: " + body.error());
                }
            } else {
                log.warn("Slack API returned non-2xx status: {}", response.getStatusCode());
                throw new RuntimeException("Slack API 호출 실패");
            }
            
        } catch (Exception e) {
            log.error("Failed to send Slack DM to user {}: {}", slackUserId, e.getMessage());
            throw new RuntimeException("Slack 메시지 전송 실패", e);
        }
    }

    public record SlackUserLookupResponse(
        @JsonProperty("ok") boolean ok,
        @JsonProperty("user") SlackUser user,
        @JsonProperty("error") String error
    ) {}

    public record SlackUser(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("profile") SlackProfile profile
    ) {}

    public record SlackProfile(
        @JsonProperty("email") String email,
        @JsonProperty("real_name") String realName
    ) {}

    public record SlackChatMessage(
        @JsonProperty("channel") String channel,
        @JsonProperty("text") String text
    ) {}

    public record SlackChatResponse(
        @JsonProperty("ok") boolean ok,
        @JsonProperty("error") String error,
        @JsonProperty("ts") String ts
    ) {}
}