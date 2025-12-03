package dsn.webmail.service;

import dsn.webmail.dto.MapDtos.NaverBlogResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class NaverSearchApiClient {

    @Value("${naver.api.client-id:}")
    private String clientId;

    @Value("${naver.api.client-secret:}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "https://openapi.naver.com/v1/search";

    /**
     * 블로그 검색
     * @param query 검색어
     * @param display 검색 결과 개수 (최대 100)
     * @return 블로그 검색 결과
     */
    public NaverBlogResponse searchBlog(String query, int display) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/blog.json")
                    .queryParam("query", encodedQuery)
                    .queryParam("display", display)
                    .queryParam("sort", "sim")  // sim: 정확도순, date: 날짜순
                    .build(true)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", clientId);
            headers.set("X-Naver-Client-Secret", clientSecret);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<NaverBlogResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    NaverBlogResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Naver API 호출 실패: {}", e.getMessage());
            return new NaverBlogResponse();
        }
    }

    /**
     * 맛집 리뷰 검색
     * @param restaurantName 식당명
     * @param address 주소 (구/동 등)
     * @return 블로그 리뷰 결과
     */
    public NaverBlogResponse searchRestaurantReviews(String restaurantName, String address) {
        // 주소에서 구/동 추출하여 검색어 구성
        String searchQuery = restaurantName + " " + extractDistrict(address) + " 맛집";
        return searchBlog(searchQuery, 5);
    }

    private String extractDistrict(String address) {
        if (address == null || address.isEmpty()) {
            return "";
        }
        // "서울 강남구 역삼로9길 30" -> "강남구"
        String[] parts = address.split(" ");
        for (String part : parts) {
            if (part.endsWith("구") || part.endsWith("동")) {
                return part;
            }
        }
        return parts.length > 1 ? parts[1] : "";
    }
}
