package dsn.webmail.controller;

import dsn.webmail.dto.SettingsDtos.*;
import dsn.webmail.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Settings", description = "설정 API")
@Controller
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * 설정 페이지
     */
    @GetMapping("/setup")
    public String setupPage() {
        return "setup/index";
    }

    /**
     * 설정 조회 API
     */
    @Operation(summary = "설정 조회", description = "사용자 설정 정보를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/settings")
    @ResponseBody
    public ResponseEntity<SettingsResponse> getSettings(
            @AuthenticationPrincipal String email) {
        SettingsResponse response = settingsService.getSettings(email);
        return ResponseEntity.ok(response);
    }

    /**
     * Slack 알람 설정 변경 API
     */
    @Operation(summary = "Slack 알람 설정 변경", description = "Slack 알람 수신 여부를 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/api/settings/slack-notification")
    @ResponseBody
    public ResponseEntity<SlackNotificationResponse> updateSlackNotification(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody SlackNotificationRequest request) {
        SlackNotificationResponse response = settingsService.updateSlackNotification(
                email, request.enabled());
        return ResponseEntity.ok(response);
    }
}
