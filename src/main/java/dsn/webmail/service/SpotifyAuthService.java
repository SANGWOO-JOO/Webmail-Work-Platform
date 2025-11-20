package dsn.webmail.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
public class SpotifyAuthService {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    private String accessToken;
    private LocalDateTime tokenExpiry;

    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";

    /**
     * 액세스 토큰 획득 (Client Credentials Flow)
     */
    public String getAccessToken() {
        if (accessToken != null && tokenExpiry != null && LocalDateTime.now().isBefore(tokenExpiry)) {
            return accessToken;
        }

        try {
            String credentials = clientId + ":" + clientSecret;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encodedCredentials);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                accessToken = (String) responseBody.get("access_token");
                int expiresIn = (Integer) responseBody.get("expires_in");
                tokenExpiry = LocalDateTime.now().plusSeconds(expiresIn - 60); // 1분 여유

                log.info("Spotify 액세스 토큰 획득 완료, 만료: {}", tokenExpiry);
                return accessToken;
            }

            throw new RuntimeException("Spotify 토큰 획득 실패");
        } catch (Exception e) {
            log.error("Spotify 인증 오류: {}", e.getMessage());
            throw new RuntimeException("Spotify 인증 실패", e);
        }
    }
}
