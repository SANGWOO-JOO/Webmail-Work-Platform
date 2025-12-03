package dsn.webmail.service;

import dsn.webmail.dto.AuthDtos.LoginResponse;
import dsn.webmail.dto.AuthDtos.PasswordResetResponse;
import dsn.webmail.dto.AuthDtos.RefreshTokenResponse;
import dsn.webmail.entity.AppUser;
import dsn.webmail.entity.PasswordResetToken;
import dsn.webmail.repository.AppUserRepository;
import dsn.webmail.repository.PasswordResetTokenRepository;
import dsn.webmail.security.JwtTokenProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class AuthService {

    private final AppUserRepository userRepo;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final MailSendService mailSendService;

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

    // ===== 비밀번호 재설정 =====

    /**
     * 인증코드 발송
     */
    @Transactional
    public PasswordResetResponse sendPasswordResetCode(String email) {
        // 1. 사용자 존재 확인
        AppUser user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 이메일입니다."));

        // 2. 기존 토큰 삭제
        resetTokenRepository.deleteByEmail(email);

        // 3. 6자리 인증코드 생성
        String code = generateVerificationCode();
        String codeHash = passwordEncoder.encode(code);

        // 4. 토큰 저장 (5분 유효)
        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .codeHash(codeHash)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        resetTokenRepository.save(token);

        // 5. 이메일 발송
        String subject = "[MTS] 비밀번호 재설정 인증코드";
        String content = String.format(
                "안녕하세요.\n\n" +
                "비밀번호 재설정을 위한 인증코드입니다.\n\n" +
                "인증코드: %s\n\n" +
                "이 코드는 5분간 유효합니다.\n" +
                "본인이 요청하지 않으셨다면 이 이메일을 무시해주세요.\n\n" +
                "감사합니다.\n" +
                "MTS 팀",
                code
        );

        try {
            mailSendService.sendEmail(email, subject, content);
            log.info("Password reset code sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }

        return new PasswordResetResponse(true, "인증코드가 이메일로 발송되었습니다.");
    }

    /**
     * 인증코드 확인
     */
    @Transactional
    public PasswordResetResponse verifyResetCode(String email, String code) {
        // 1. 토큰 조회
        PasswordResetToken token = resetTokenRepository
                .findByEmailAndVerifiedFalseAndExpiresAtAfter(email, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("인증코드가 만료되었거나 존재하지 않습니다."));

        // 2. 시도 횟수 확인
        if (token.getAttemptCount() >= 5) {
            throw new IllegalStateException("인증 시도 횟수를 초과했습니다. 새 인증코드를 요청해주세요.");
        }

        // 3. 코드 검증
        if (!passwordEncoder.matches(code, token.getCodeHash())) {
            token.incrementAttemptCount();
            resetTokenRepository.save(token);
            throw new IllegalArgumentException("인증코드가 일치하지 않습니다.");
        }

        // 4. 인증 완료 처리
        token.setVerified(true);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10)); // 비밀번호 설정을 위해 10분 연장
        resetTokenRepository.save(token);

        // 5. resetToken 생성 (JWT)
        String resetToken = tokenProvider.createPasswordResetToken(email);

        log.info("Password reset code verified for: {}", email);

        return new PasswordResetResponse(true, "인증이 완료되었습니다.", resetToken);
    }

    /**
     * 비밀번호 재설정
     */
    @Transactional
    public PasswordResetResponse resetPassword(String email, String resetToken, String newPassword) {
        // 1. resetToken 검증
        if (!tokenProvider.validateToken(resetToken)) {
            throw new IllegalArgumentException("유효하지 않은 요청입니다.");
        }

        String tokenEmail = tokenProvider.getEmailFromToken(resetToken);
        if (!email.equals(tokenEmail)) {
            throw new IllegalArgumentException("유효하지 않은 요청입니다.");
        }

        // 2. 인증된 토큰 확인
        Optional<PasswordResetToken> verifiedToken = resetTokenRepository
                .findByEmailAndVerifiedTrueAndExpiresAtAfter(email, LocalDateTime.now());

        if (verifiedToken.isEmpty()) {
            throw new IllegalArgumentException("인증이 완료되지 않았거나 만료되었습니다.");
        }

        // 3. 비밀번호 유효성 검사
        validatePassword(newPassword);

        // 4. 사용자 비밀번호 업데이트
        AppUser user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setLoginFailureCount(0); // 실패 횟수 초기화
        if (user.getStatus() == AppUser.Status.LOCKED) {
            user.setStatus(AppUser.Status.ACTIVE); // 잠금 해제
        }
        userRepo.save(user);

        // 5. 사용된 토큰 삭제
        resetTokenRepository.deleteByEmail(email);

        log.info("Password reset successfully for: {}", email);

        return new PasswordResetResponse(true, "비밀번호가 성공적으로 변경되었습니다.");
    }

    // ===== 유틸리티 =====

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // 6자리 숫자
        return String.valueOf(code);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
        }

        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        if (!hasLetter || !hasDigit || !hasSpecial) {
            throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.");
        }
    }
}
