package dsn.webmail.service;

import dsn.webmail.dto.MapDtos.KakaoPlaceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class KakaoLocalApiClient {

    @Value("${kakao.api.key:}")
    private String apiKey;

    @Value("${company.location.longitude:127.036339}")
    private String companyLongitude;

    @Value("${company.location.latitude:37.500087}")
    private String companyLatitude;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "https://dapi.kakao.com/v2/local";

    /**
     * 카테고리로 장소 검색
     * @param categoryGroupCode FD6(음식점), CE7(카페) 등
     * @param radius 반경 (미터)
     * @param page 페이지 번호
     * @return 장소 검색 결과
     *
     * Note: sort=accuracy 사용 이유
     * - sort=distance는 Kakao API에서 최대 45개(3페이지)만 반환하는 제한이 있음
     * - sort=accuracy는 최대 675개(45페이지)까지 조회 가능
     * - 거리 정보는 응답의 distance 필드에 포함되므로 클라이언트에서 정렬 가능
     */
    public KakaoPlaceResponse searchByCategory(String categoryGroupCode, int radius, int page) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/search/category.json")
                .queryParam("category_group_code", categoryGroupCode)
                .queryParam("x", companyLongitude)
                .queryParam("y", companyLatitude)
                .queryParam("radius", radius)
                .queryParam("sort", "accuracy")  // distance는 최대 45개 제한, accuracy는 675개 가능
                .queryParam("page", page)
                .queryParam("size", 15)
                .build()
                .toUriString();

        return executeRequest(url);
    }

    /**
     * 키워드로 장소 검색
     * @param keyword 검색 키워드
     * @param radius 반경 (미터)
     * @return 장소 검색 결과
     */
    public KakaoPlaceResponse searchByKeyword(String keyword, int radius) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/search/keyword.json")
                .queryParam("query", keyword)
                .queryParam("x", companyLongitude)
                .queryParam("y", companyLatitude)
                .queryParam("radius", radius)
                .queryParam("sort", "distance")
                .queryParam("size", 15)
                .build()
                .toUriString();

        return executeRequest(url);
    }

    private KakaoPlaceResponse executeRequest(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + apiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<KakaoPlaceResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    KakaoPlaceResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Kakao API 호출 실패: {}", e.getMessage());
            return new KakaoPlaceResponse();
        }
    }
}
