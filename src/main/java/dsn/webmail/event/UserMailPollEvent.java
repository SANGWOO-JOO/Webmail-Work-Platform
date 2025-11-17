package dsn.webmail.event;

import java.time.LocalDateTime;

/**
 * 사용자별 메일 폴링 이벤트
 * @param userId 처리할 사용자 ID
 * @param requestedAt 이벤트 발행 시각
 */
public record UserMailPollEvent(
    Long userId,
    LocalDateTime requestedAt
) { }
