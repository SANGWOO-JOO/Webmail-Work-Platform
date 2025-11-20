package dsn.webmail.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SpotifyApiClient {

    private final SpotifyAuthService spotifyAuthService;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String API_BASE_URL = "https://api.spotify.com/v1";

    /**
     * 음악 추천 받기
     */
    public Map<String, Object> getRecommendations(
            List<String> seedGenres,
            Float targetEnergy,
            Float targetValence,
            Integer targetTempo,
            Float targetAcousticness,
            int limit) {

        String url = UriComponentsBuilder.fromHttpUrl(API_BASE_URL + "/recommendations")
                .queryParam("seed_genres", String.join(",", seedGenres))
                .queryParam("limit", limit)
                .queryParamIfPresent("target_energy", java.util.Optional.ofNullable(targetEnergy))
                .queryParamIfPresent("target_valence", java.util.Optional.ofNullable(targetValence))
                .queryParamIfPresent("target_tempo", java.util.Optional.ofNullable(targetTempo))
                .queryParamIfPresent("target_acousticness", java.util.Optional.ofNullable(targetAcousticness))
                .toUriString();

        log.debug("Spotify 추천 요청: {}", url);

        return executeRequest(url);
    }

    /**
     * 트랙 검색
     */
    public Map<String, Object> searchTracks(String query, int limit) {
        String url = UriComponentsBuilder.fromHttpUrl(API_BASE_URL + "/search")
                .queryParam("q", query)
                .queryParam("type", "track")
                .queryParam("limit", limit)
                .queryParam("market", "KR")
                .toUriString();

        log.debug("Spotify 검색 요청: {}", url);

        return executeRequest(url);
    }

    /**
     * 사용 가능한 장르 시드 목록
     */
    public List<String> getAvailableGenreSeeds() {
        String url = API_BASE_URL + "/recommendations/available-genre-seeds";

        Map<String, Object> response = executeRequest(url);
        return (List<String>) response.get("genres");
    }

    private Map<String, Object> executeRequest(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(spotifyAuthService.getAccessToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }

            throw new RuntimeException("Spotify API 요청 실패: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("Spotify API 오류: {}", e.getMessage());
            throw new RuntimeException("Spotify API 요청 실패", e);
        }
    }
}
