package dsn.webmail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public class SettingsDtos {

    @Schema(description = "설정 정보 응답")
    public record SettingsResponse(
            @Schema(description = "사용자 이메일")
            String email,

            @Schema(description = "Slack 알람 수신 여부")
            Boolean slackNotificationEnabled,

            @Schema(description = "Slack 사용자 ID")
            String slackUserId,

            @Schema(description = "메일 폴링 활성화 여부")
            Boolean mailPollingEnabled
    ) {
    }

    @Schema(description = "Slack 알람 설정 변경 요청")
    public record SlackNotificationRequest(
            @NotNull
            @Schema(description = "알람 활성화 여부", example = "true")
            Boolean enabled
    ) {
    }

    @Schema(description = "Slack 알람 설정 변경 응답")
    public record SlackNotificationResponse(
            @Schema(description = "성공 여부")
            Boolean success,

            @Schema(description = "변경된 알람 설정 상태")
            Boolean slackNotificationEnabled,

            @Schema(description = "응답 메시지")
            String message
    ) {
    }
}
