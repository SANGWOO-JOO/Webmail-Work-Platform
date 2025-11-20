package dsn.webmail.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dsn.webmail.dto.HolidayDtos.HolidayListResponse;
import dsn.webmail.dto.HolidayDtos.HolidayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class HolidayService {

    @Value("${public-data.holiday.service-key}")
    private String serviceKey;

    @Value("${public-data.holiday.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // 간단한 메모리 캐시 (key: "yyyy-MM")
    private final Map<String, HolidayListResponse> cache = new ConcurrentHashMap<>();

    public HolidayService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 월별 공휴일 조회
     */
    public HolidayListResponse getHolidaysByMonth(int year, int month) {
        String cacheKey = String.format("%d-%02d", year, month);

        // 캐시 확인
        if (cache.containsKey(cacheKey)) {
            log.debug("공휴일 캐시 히트: {}", cacheKey);
            return cache.get(cacheKey);
        }

        List<HolidayResponse> holidays = new ArrayList<>();

        try {
            // 공휴일 정보 조회
            List<HolidayResponse> holiDeInfo = fetchHolidays(year, month, "getHoliDeInfo");
            holidays.addAll(holiDeInfo);

            log.info("공휴일 조회 완료: {} - {}건", cacheKey, holidays.size());
        } catch (Exception e) {
            log.error("공휴일 API 호출 실패: {}", e.getMessage());
        }

        HolidayListResponse response = new HolidayListResponse(holidays);

        // 캐시 저장
        cache.put(cacheKey, response);

        return response;
    }

    /**
     * 공공데이터포털 API 호출
     */
    private List<HolidayResponse> fetchHolidays(int year, int month, String operation) {
        List<HolidayResponse> holidays = new ArrayList<>();

        try {
            // 공공데이터포털은 서비스키를 인코딩하지 않고 그대로 전달해야 함
            String url = String.format("%s/%s?serviceKey=%s&solYear=%d&solMonth=%02d&_type=json&numOfRows=100",
                    baseUrl, operation, serviceKey, year, month);

            log.debug("공휴일 API 호출: {}", url);

            String response = restTemplate.getForObject(url, String.class);

            if (response != null) {
                holidays = parseResponse(response);
            }
        } catch (Exception e) {
            log.error("API 호출 오류 ({}): {}", operation, e.getMessage());
        }

        return holidays;
    }

    /**
     * API 응답 파싱
     */
    private List<HolidayResponse> parseResponse(String response) {
        List<HolidayResponse> holidays = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.path("response").path("body").path("items").path("item");

            if (items.isMissingNode() || items.isEmpty()) {
                return holidays;
            }

            // 단일 아이템인 경우 배열로 처리
            if (items.isObject()) {
                holidays.add(parseItem(items));
            } else if (items.isArray()) {
                for (JsonNode item : items) {
                    holidays.add(parseItem(item));
                }
            }
        } catch (Exception e) {
            log.error("응답 파싱 오류: {}", e.getMessage());
        }

        return holidays;
    }

    /**
     * 개별 아이템 파싱
     */
    private HolidayResponse parseItem(JsonNode item) {
        String locdate = item.path("locdate").asText(); // "20250101"
        String dateName = item.path("dateName").asText();
        String isHoliday = item.path("isHoliday").asText();

        // 날짜 형식 변환: "20250101" -> "2025-01-01"
        String formattedDate = String.format("%s-%s-%s",
                locdate.substring(0, 4),
                locdate.substring(4, 6),
                locdate.substring(6, 8));

        return new HolidayResponse(
                formattedDate,
                dateName,
                "Y".equals(isHoliday)
        );
    }

    /**
     * 캐시 클리어 (필요시 사용)
     */
    public void clearCache() {
        cache.clear();
        log.info("공휴일 캐시 클리어");
    }
}
