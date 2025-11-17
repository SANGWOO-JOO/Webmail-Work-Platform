package dsn.webmail.event.listener;

import dsn.webmail.entity.AppUser;
import dsn.webmail.event.UserMailPollEvent;
import dsn.webmail.repository.AppUserRepository;
import dsn.webmail.service.MailAlertService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class MailPollingEventListener {

    private final AppUserRepository userRepo;
    private final MailAlertService mailAlertService;

    /**
     * 현재 처리 중인 사용자 ID 추적
     * 동시성 제어를 위한 in-memory set:
     * - ConcurrentHashMap.newKeySet()으로 thread-safe set 생성
     * - 처리 시작 시 userId 추가
     * - 처리 완료/실패 시 userId 제거
     * - 이미 존재하면 중복 처리 방지
     */
    private final Set<Long> processingUserIds = ConcurrentHashMap.newKeySet();

    @Async("mailPollingExecutor")  // AsyncConfig에서 정의한 스레드 풀 사용
    @EventListener  // UserMailPollEvent 발행 시 자동 호출
    @Transactional  // 사용자별 독립 트랜잭션 (다른 사용자와 격리)
    public void handleUserMailPoll(UserMailPollEvent event) {
        Long userId = event.userId();
        LocalDateTime now = event.requestedAt();

        // 0. 중복 처리 방지: 이미 처리 중이면 skip
        if (!processingUserIds.add(userId)) {
            log.debug("User {} is already being processed, skipping duplicate event", userId);
            return;
        }

        try {
            // 디버그 로그: 어떤 스레드에서 실행되는지 확인
            log.debug("Processing mail for user ID: {} (thread: {})", userId, Thread.currentThread().getName());

            // 1. 사용자 조회
            AppUser user = userRepo.findById(userId).orElse(null);
            if (user == null) {
                log.warn("User not found: {}", userId);
                return;
            }

            // 2. 메일 폴링 처리 (POP3 연결 → 메일 조회 → Slack 알림)
            mailAlertService.processFor(user);

            // 3. 성공 처리: 상태 업데이트
            user.setLastCheckedAt(now);
            user.setFailureCount(0);
            user.setNextRetryAt(null);
            userRepo.save(user);

            log.debug("Mail processing completed for user: {}", user.getEmail());

        } catch (MessagingException ex) {
            // POP3 연결 오류 (인증 실패, 네트워크 오류 등)
            handleMessagingException(userId, now, ex);

        } catch (Exception e) {
            // 기타 예상치 못한 예외
            handleUnexpectedException(userId, e);

        } finally {
            // 처리 완료/실패와 관계없이 항상 제거 (다음 스케줄에서 다시 처리 가능하도록)
            processingUserIds.remove(userId);
            log.debug("User {} removed from processing set", userId);
        }
    }

    private void handleMessagingException(Long userId, LocalDateTime now, MessagingException ex) {
        try {
            AppUser user = userRepo.findById(userId).orElse(null);
            if (user == null) return;

            // 실패 횟수 증가
            int failureCount = Optional.ofNullable(user.getFailureCount()).orElse(0) + 1;
            user.setFailureCount(failureCount);

            // Exponential Backoff: 2^(n-1)분, 최대 15분
            long backoffMinutes = Math.min(15, (long) Math.pow(2, failureCount - 1));
            user.setNextRetryAt(now.plusMinutes(backoffMinutes));

            userRepo.save(user);

            log.warn("POP3 connection failed for user {} (attempt {}, retry in {}m): {}",
                    user.getEmail(), failureCount, backoffMinutes, ex.getMessage());

        } catch (Exception e) {
            log.error("Error updating failure count for user {}: {}", userId, e.getMessage());
        }
    }

    private void handleUnexpectedException(Long userId, Exception e) {
        try {
            AppUser user = userRepo.findById(userId).orElse(null);
            if (user == null) return;

            log.error("Unexpected error processing mail for user {}: {}",
                    user.getEmail(), e.getMessage(), e);

            // 실패 카운트 증가 (재시도 로직 적용)
            int failureCount = Optional.ofNullable(user.getFailureCount()).orElse(0) + 1;
            user.setFailureCount(failureCount);

            long backoffMinutes = Math.min(15, (long) Math.pow(2, failureCount - 1));
            user.setNextRetryAt(LocalDateTime.now().plusMinutes(backoffMinutes));

            userRepo.save(user);

        } catch (Exception ex) {
            log.error("Error handling exception for user {}: {}", userId, ex.getMessage());
        }
    }
}
