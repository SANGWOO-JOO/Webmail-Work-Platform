package dsn.webmail.service;

import dsn.webmail.dto.AuthDtos.LoginResponse;
import dsn.webmail.dto.AuthDtos.RefreshTokenResponse;
import dsn.webmail.entity.AppUser;
import dsn.webmail.repository.AppUserRepository;
import dsn.webmail.security.JwtTokenProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@AllArgsConstructor
public class AuthService {

    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public LoginResponse login(String email, String rawPassword) {

        // 1. 사용자 조회
        AppUser user = userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.")
                );

        // 2. 상태 확인
        if (user.getStatus() == AppUser.Status.LOCKED) {
            throw new IllegalStateException("계정이 잠겼습니다. 관리자에게 문의하세요.");
        }

        if (user.getStatus() != AppUser.Status.ACTIVE) {
            throw new IllegalStateException("회원가입이 완료되지 않았습니다.");
        }

        if (!user.getWebLoginEnabled()) {
            throw new IllegalStateException("웹 로그인이 비활성화되었습니다.");
        }

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            // 실패 횟수 증가
            user.setLoginFailureCount(user.getLoginFailureCount() + 1);

            if (user.getLoginFailureCount() >= 5) {
                user.setStatus(AppUser.Status.LOCKED);
                userRepo.save(user);
                throw new IllegalStateException("로그인 5회 실패로 계정이 잠겼습니다.");
            }

            userRepo.save(user);
            throw new IllegalArgumentException(
                    "이메일 또는 비밀번호가 올바르지 않습니다. (" +
                            user.getLoginFailureCount() + "/5)"
            );
        }

        // 4. 로그인 성공 처리
        user.setLoginFailureCount(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepo.save(user);

        // 5. JWT 생성
        String accessToken = tokenProvider.createAccessToken(
                user.getEmail(),
                user.getRole().name()
        );

        String refreshToken = tokenProvider.createRefreshToken(user.getEmail());

        log.info("User logged in: {}", email);

        // 6. 응답 반환
        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                tokenProvider.getAccessTokenValidityInSeconds(),
                user.getEmail()
        );
    }

    /**
     * Refresh Token으로 새 Access Token 발급
     */
    @Transactional(readOnly = true)
    public RefreshTokenResponse refresh(String refreshToken) {

        // 1. Refresh Token 검증
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        if (!tokenProvider.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token이 아닙니다.");
        }

        // 2. 사용자 조회
        String email = tokenProvider.getEmailFromToken(refreshToken);
        AppUser user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 3. 상태 확인
        if (user.getStatus() != AppUser.Status.ACTIVE) {
            throw new IllegalStateException("비활성화된 계정입니다.");
        }

        // 4. 새 Access Token 생성
        String newAccessToken = tokenProvider.createAccessToken(
                user.getEmail(),
                user.getRole().name()
        );

        return new RefreshTokenResponse(
                newAccessToken,
                "Bearer",
                tokenProvider.getAccessTokenValidityInSeconds()
        );
    }
}
