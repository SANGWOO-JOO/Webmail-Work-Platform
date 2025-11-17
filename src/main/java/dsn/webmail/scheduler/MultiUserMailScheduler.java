package dsn.webmail.scheduler;

import dsn.webmail.entity.AppUser;
import dsn.webmail.event.UserMailPollEvent;
import dsn.webmail.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 다중 사용자 메일 폴링 스케줄러
 * <p>
 * 변경 사항:
 * - 기존: 순차 처리 (for문으로 직접 MailAlertService 호출)
 * - 개선: 이벤트 발행 → EventListener에서 비동기 병렬 처리
 * <p>
 * 장점:
 * - 10명 처리 시간: 100초 → 10초 (10배 개선)
 * - 장애 격리: 1명 실패해도 다른 사용자는 정상 처리
 * - 스레드 풀 관리: AsyncConfig에서 중앙 관리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MultiUserMailScheduler {

    private final AppUserRepository userRepo;
    private final ApplicationEventPublisher eventPublisher;  // 이벤트 발행자 추가


    /**
     * 전체 사용자 메일 폴링 스케줄링
     * <p>
     * 동작 방식 (변경됨):
     * 1. 활성 사용자 목록 조회
     * 2. 사용자별 UserMailPollEvent 발행
     * 3. EventListener에서 비동기로 병렬 처리
     * 4. 즉시 반환 (백그라운드에서 실행)
     * <p>
     * 기존 방식과의 차이:
     * - 기존: for문으로 순차 처리 (10명 = 100초)
     * - 개선: 이벤트 발행 후 즉시 반환 (10명 = 10초)
     */
    @Scheduled(fixedDelayString = "#{@mailPop3Properties.pollIntervalMs()}")
    public void pollAll() {
        LocalDateTime now = LocalDateTime.now();
        List<AppUser> users = userRepo.findActiveForPolling(now);

        // 각 사용자별로 이벤트 발행 (비동기 처리)
        users.forEach(user -> {
            try {
                // UserMailPollEvent 발행 → MailPollingEventListener가 받아서 처리
                eventPublisher.publishEvent(new UserMailPollEvent(user.getId(), now));
                log.debug("Published poll event for user: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to publish event for user {}: {}", user.getEmail(), e.getMessage());
            }
        });

        log.info("Published {} mail poll events (processing in background)", users.size());
        // 여기서 즉시 반환! 실제 처리는 MailPollingEventListener에서 병렬로 진행됨
    }
}