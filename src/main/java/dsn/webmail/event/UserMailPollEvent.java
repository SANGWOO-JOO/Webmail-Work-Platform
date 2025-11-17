package dsn.webmail.event;

import java.time.LocalDateTime;

/**
 * 사용자별 메일 폴링 이벤트
 *
 * 스케줄러에서 발행하여 각 사용자의 메일을 비동기로 처리하도록 합니다.
 *
 * @param userId 처리할 사용자 ID
 * @param requestedAt 이벤트 발행 시각
 */
public record UserMailPollEvent(
    Long userId,
    LocalDateTime requestedAt
) {
    /**
     * 현재 시각으로 이벤트 생성
     */
    public UserMailPollEvent(Long userId) {
        this(userId, LocalDateTime.now());
    }
}
