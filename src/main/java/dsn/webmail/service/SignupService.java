package dsn.webmail.service;

import dsn.webmail.entity.AppUser;
import dsn.webmail.entity.EmailVerification;
import dsn.webmail.repository.AppUserRepository;
import dsn.webmail.repository.EmailVerificationRepository;
import dsn.webmail.util.PasswordCipher;
import dsn.webmail.util.VerificationCodeGenerator;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

@Service
@Slf4j
public class SignupService {
    
    private final AppUserRepository userRepo;
    private final EmailVerificationRepository verRepo;
    private final VerificationCodeGenerator generator;
    private final MailSenderService mailSender;
    private final PasswordEncoder passwordEncoder;
    private final PasswordCipher passwordCipher;
    private final SlackApiClient slackApiClient;

    public SignupService(AppUserRepository userRepo, 
                        EmailVerificationRepository verRepo,
                        VerificationCodeGenerator generator, 
                        MailSenderService mailSender,
                        PasswordEncoder passwordEncoder, 
                        PasswordCipher passwordCipher,
                        SlackApiClient slackApiClient) {
        this.userRepo = userRepo;
        this.verRepo = verRepo;
        this.generator = generator;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.passwordCipher = passwordCipher;
        this.slackApiClient = slackApiClient;
    }

    @Transactional
    public void requestCode(String name, String email, String rawPop3Password, String rawWebPassword) {
        // 1) 중복 이메일 방지(이미 ACTIVE면 거절)
        userRepo.findByEmail(email).ifPresent(u -> {
            if (u.getStatus() == AppUser.Status.ACTIVE) {
                throw new IllegalStateException("이미 가입된 이메일입니다.");
            }
        });

        // 2) 재전송 쿨다운 체크(60초) - 트랜잭션 내에서 먼저 체크
        var last = verRepo.findTopByEmailOrderByIdDesc(email).orElse(null);
        if (last != null && last.getLastSentAt() != null &&
            last.getLastSentAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
            throw new IllegalStateException("코드는 60초 후에 다시 요청 가능합니다.");
        }

        // 3) 사용자 엔티티 준비(PENDING)
        try {
            AppUser user = userRepo.findByEmail(email).orElseGet(() -> {
                AppUser newUser = new AppUser();
                newUser.setEmail(email);
                return newUser;
            });
            user.setName(name);
            user.setEncryptedPop3Password(passwordCipher.encrypt(rawPop3Password));

            // 웹 로그인 패스워드 해시 저장
            user.setPasswordHash(passwordEncoder.encode(rawWebPassword));
            user.setWebLoginEnabled(true);

            user.setStatus(AppUser.Status.PENDING);
            userRepo.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Handle unique constraint violation (concurrent user creation)
            log.warn("Concurrent user creation attempt for email: {}", email);
            throw new IllegalStateException("이미 처리 중인 요청입니다. 잠시 후 다시 시도해주세요.");
        }

        // 4) 코드 생성 & 해시 저장(유효시간 10분)
        String code = generator.sixDigits();
        EmailVerification ver = new EmailVerification();
        ver.setEmail(email);
        ver.setCodeHash(passwordEncoder.encode(code));
        ver.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        ver.setAttempts(0);
        ver.setLastSentAt(LocalDateTime.now());
        verRepo.save(ver);

        // 5) 메일 발송 (사용자 자신의 계정에서 자신에게)
        try {
            mailSender.sendVerificationCode(email, rawPop3Password, code);
        } catch (Exception e) {
            log.error("Failed to send verification code for {}: {}", email, e.getMessage());
            // 메일 발송 실패 시에도 트랜잭션 롤백하지 않고 재시도 가능하도록
            throw new RuntimeException("인증 메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    @Transactional
    public void verify(String email, String inputCode) {
        EmailVerification ver = verRepo.findTopByEmailOrderByIdDesc(email)
                .orElseThrow(() -> new IllegalStateException("인증 요청이 없습니다."));

        if (ver.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("코드가 만료되었습니다.");
        }
        if (ver.getAttempts() >= 5) {
            throw new IllegalStateException("시도 횟수를 초과했습니다.");
        }

        ver.setAttempts(ver.getAttempts() + 1);

        // 코드 비교(해시 매칭)
        if (!passwordEncoder.matches(inputCode, ver.getCodeHash())) {
            verRepo.save(ver);
            throw new IllegalArgumentException("코드가 일치하지 않습니다.");
        }

        // 성공 → 사용자 활성화
        AppUser user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
        user.setStatus(AppUser.Status.ACTIVE);
        user.setMailPollingEnabled(true);
        
        // Slack User ID 조회 및 저장
        try {
            String slackUserId = slackApiClient.lookupUserIdByEmail(email);
            if (slackUserId != null) {
                user.setSlackUserId(slackUserId);
                log.info("Slack User ID {} set for user {}", slackUserId, email);
            } else {
                log.warn("Could not find Slack User ID for email: {}", email);
            }
        } catch (Exception e) {
            log.warn("Failed to lookup Slack User ID for {}: {}", email, e.getMessage());
        }
        
        userRepo.save(user);

        // 인증 레코드 정리
        verRepo.deleteByEmail(email);
    }
}