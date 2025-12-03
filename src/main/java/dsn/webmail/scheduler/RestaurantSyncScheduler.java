package dsn.webmail.scheduler;

import dsn.webmail.service.MapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestaurantSyncScheduler {

    private final MapService mapService;

    /**
     * 매일 12시에 맛집 데이터 동기화
     */
//    @Scheduled(cron = "0 0 12 * * *")
    @Scheduled(cron = "0 10 1 * * *")
    public void syncRestaurantData() {
        log.info("맛집 데이터 동기화 스케줄러 실행");
        try {
            mapService.syncRestaurantData();
            log.info("맛집 데이터 동기화 완료");
        } catch (Exception e) {
            log.error("맛집 데이터 동기화 실패: {}", e.getMessage(), e);
        }
    }
}