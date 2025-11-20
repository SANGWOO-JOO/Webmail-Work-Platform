package dsn.webmail.dto;

import java.time.LocalDateTime;

public class DashboardDtos {

    public record DashboardStatsResponse(
            long todayMailCount,
            long totalMailCount,
            String slackNotificationStatus
    ) {}

    public record AccountInfoResponse(
            String email,
            String accountStatus,
            String mailPollingStatus,
            LocalDateTime createdAt
    ) {}
}
