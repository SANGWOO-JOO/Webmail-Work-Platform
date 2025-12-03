package dsn.webmail.service;

import dsn.webmail.dto.SettingsDtos.SettingsResponse;
import dsn.webmail.dto.SettingsDtos.SlackNotificationResponse;
import dsn.webmail.entity.AppUser;
import dsn.webmail.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SettingsService {

    private final AppUserRepository userRepository;

    /**
     * 사용자 설정 조회
     */
    @Transactional(readOnly = true)
    public SettingsResponse getSettings(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return new SettingsResponse(
                user.getEmail(),
                user.getSlackNotificationEnabled(),
                user.getSlackUserId(),
                user.getMailPollingEnabled()
        );
    }

    /**
     * Slack 알람 설정 변경
     */
    @Transactional
    public SlackNotificationResponse updateSlackNotification(String email, boolean enabled) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.setSlackNotificationEnabled(enabled);
        userRepository.save(user);

        String message = enabled ? "알람 수신이 활성화되었습니다." : "알람 수신이 비활성화되었습니다.";
        log.info("User {} slack notification changed to: {}", email, enabled);

        return new SlackNotificationResponse(true, enabled, message);
    }
}
