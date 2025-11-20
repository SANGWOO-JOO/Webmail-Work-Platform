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

    // Spotify에서 지원하는 장르 시드 목록
    private static final java.util.Set<String> VALID_GENRES = java.util.Set.of(
            "acoustic", "afrobeat", "alt-rock", "alternative", "ambient", "anime", "black-metal",
            "bluegrass", "blues", "bossanova", "brazil", "breakbeat", "british", "cantopop",
            "chicago-house", "children", "chill", "classical", "club", "comedy", "country",
            "dance", "dancehall", "death-metal", "deep-house", "detroit-techno", "disco",
            "disney", "drum-and-bass", "dub", "dubstep", "edm", "electro", "electronic", "emo",
            "folk", "forro", "french", "funk", "garage", "german", "gospel", "goth", "grindcore",
            "groove", "grunge", "guitar", "happy", "hard-rock", "hardcore", "hardstyle",
            "heavy-metal", "hip-hop", "holidays", "honky-tonk", "house", "idm", "indian",
            "indie", "indie-pop", "industrial", "iranian", "j-dance", "j-idol", "j-pop", "j-rock",
            "jazz", "k-pop", "kids", "latin", "latino", "malay", "mandopop", "metal", "metal-misc",
            "metalcore", "minimal-techno", "movies", "mpb", "new-age", "new-release", "opera",
            "pagode", "party", "philippines-opm", "piano", "pop", "pop-film", "post-dubstep",
            "power-pop", "progressive-house", "psych-rock", "punk", "punk-rock", "r-n-b",
            "rainy-day", "reggae", "reggaeton", "road-trip", "rock", "rock-n-roll", "rockabilly",
            "romance", "sad", "salsa", "samba", "sertanejo", "show-tunes", "singer-songwriter",
            "ska", "sleep", "songwriter", "soul", "soundtracks", "spanish", "study", "summer",
            "swedish", "synth-pop", "tango", "techno", "trance", "trip-hop", "turkish",
            "work-out", "world-music"
    );

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

        // 유효한 장르만 필터링
        List<String> validGenres = seedGenres.stream()
                .filter(VALID_GENRES::contains)
                .limit(5)
                .toList();

        if (validGenres.isEmpty()) {
            validGenres = List.of("pop", "k-pop"); // 기본 장르
        }

        var builder = UriComponentsBuilder.fromHttpUrl(API_BASE_URL + "/recommendations")
                .queryParam("seed_genres", String.join(",", validGenres))
                .queryParam("limit", limit);

        if (targetEnergy != null) {
            builder.queryParam("target_energy", targetEnergy);
        }
        if (targetValence != null) {
            builder.queryParam("target_valence", targetValence);
        }
        if (targetTempo != null) {
            builder.queryParam("target_tempo", targetTempo);
        }
        if (targetAcousticness != null) {
            builder.queryParam("target_acousticness", targetAcousticness);
        }

        String url = builder.toUriString();
        log.debug("Spotify 추천 요청: {}", url);

        return executeRequest(url);
    }

    /**
     * 트랙 검색
     */
    public Map<String, Object> searchTracks(String query, int limit) {
        return searchTracks(query, limit, "KR");
    }

    /**
     * 트랙 검색 (국가 지정)
     */
    public Map<String, Object> searchTracks(String query, int limit, String market) {
        // 랜덤 offset으로 매번 다른 결과 반환 (0~50 사이)
        int randomOffset = new java.util.Random().nextInt(50);

        String url = UriComponentsBuilder.fromHttpUrl(API_BASE_URL + "/search")
                .queryParam("q", query)
                .queryParam("type", "track")
                .queryParam("limit", limit)
                .queryParam("offset", randomOffset)
                .queryParam("market", market)
                .toUriString();

        log.debug("Spotify 검색 요청: {}", url);

        return executeRequest(url);
    }

    /**
     * 아티스트 검색
     */
    public Map<String, Object> searchArtist(String artistName) {
        String url = UriComponentsBuilder.fromHttpUrl(API_BASE_URL + "/search")
                .queryParam("q", artistName)
                .queryParam("type", "artist")
                .queryParam("limit", 1)
                .queryParam("market", "KR")
                .toUriString();

        log.debug("Spotify 아티스트 검색: {}", url);

        return executeRequest(url);
    }

    /**
     * 아티스트의 인기 트랙 가져오기
     */
    public Map<String, Object> getArtistTopTracks(String artistId) {
        String url = UriComponentsBuilder.fromHttpUrl(API_BASE_URL + "/artists/" + artistId + "/top-tracks")
                .queryParam("market", "KR")
                .toUriString();

        log.debug("Spotify 아티스트 인기 트랙: {}", url);

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
