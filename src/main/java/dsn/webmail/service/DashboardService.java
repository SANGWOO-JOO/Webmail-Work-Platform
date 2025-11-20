package dsn.webmail.service;

import dsn.webmail.dto.DashboardDtos.AccountInfoResponse;
import dsn.webmail.dto.DashboardDtos.DashboardStatsResponse;
import dsn.webmail.entity.AppUser;
import dsn.webmail.repository.AppUserRepository;
import dsn.webmail.repository.ProcessedMailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService {

    private final AppUserRepository appUserRepository;
    private final ProcessedMailRepository processedMailRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(String email) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        // 오늘 00:00:00부터의 메일 개수
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        long todayMailCount = processedMailRepository.countByUserIdAndProcessedAtAfter(user.getId(), todayStart);

        // 총 메일 개수
        long totalMailCount = processedMailRepository.countByUserId(user.getId());

        // Slack 알림 상태
        String slackStatus = user.getSlackUserId() != null ? "활성화" : "비활성화";

        return new DashboardStatsResponse(todayMailCount, totalMailCount, slackStatus);
    }

    @Transactional(readOnly = true)
    public AccountInfoResponse getAccountInfo(String email) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        String accountStatus = switch (user.getStatus()) {
            case ACTIVE -> "활성화";
            case PENDING -> "대기중";
            case LOCKED -> "잠김";
        };

        String mailPollingStatus = user.getMailPollingEnabled() ? "활성화" : "비활성화";

        return new AccountInfoResponse(
                user.getEmail(),
                accountStatus,
                mailPollingStatus,
                user.getCreatedAt()
        );
    }
}
