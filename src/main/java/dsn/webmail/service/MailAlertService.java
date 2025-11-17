package dsn.webmail.service;

import dsn.webmail.dto.MailSummary;
import dsn.webmail.entity.AppUser;
import dsn.webmail.entity.ProcessedMail;
import dsn.webmail.repository.ProcessedMailRepository;
import dsn.webmail.util.PasswordCipher;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class MailAlertService {

    private final MailReceiver mailReceiver;
    private final SlackBotClient slackBotClient;
    private final ProcessedMailRepository processedMailRepo;
    private final PasswordCipher passwordCipher;

    public MailAlertService(MailReceiver mailReceiver,
                            SlackBotClient slackBotClient,
                            ProcessedMailRepository processedMailRepo,
                            PasswordCipher passwordCipher) {
        this.mailReceiver = mailReceiver;
        this.slackBotClient = slackBotClient;
        this.processedMailRepo = processedMailRepo;
        this.passwordCipher = passwordCipher;
    }

    @Transactional
    public void processFor(AppUser user) throws MessagingException {
        log.debug("Processing mail for user: {}", user.getEmail());

        String decryptedPassword = passwordCipher.decrypt(user.getEncryptedPop3Password());
        List<MailSummary> mails = mailReceiver.fetchRecent(user.getEmail(), decryptedPassword);

        int newMailCount = 0;
        LocalDateTime cutOffTime = user.getActivatedAt();

        for (MailSummary mail : mails) {

            if (mail.receivedDate().isBefore(cutOffTime)) {
                continue;
            }

            if (!processedMailRepo.existsByUserIdAndMessageId(user.getId(), mail.messageId())) {
                sendSlackNotification(user, mail);
                saveProcessedMail(user.getId(), mail);
                newMailCount++;
            }
        }

        if (newMailCount > 0) {
            log.info("Processed {} new mails for user: {}", newMailCount, user.getEmail());
        } else {
            log.debug("No new mails for user: {}", user.getEmail());
        }
    }

    private void sendSlackNotification(AppUser user, MailSummary mail) {
        if (user.getSlackUserId() == null) {
            log.warn("No Slack User ID for user: {}, skipping notification", user.getEmail());
            return;
        }

        try {
            String message = formatSlackMessage(user.getEmail(), mail);
            slackBotClient.sendDirectMessage(user.getSlackUserId(), message);
            log.debug("Slack notification sent for mail: {}", mail.messageId());
        } catch (Exception e) {
            // Slack 알림 실패는 비치명적 에러로 처리
            // 메일은 정상적으로 처리되고 processed_mail에 저장되어야 함
            log.error("Failed to send Slack notification for user {}, mail {}: {} (continuing mail processing)",
                    user.getEmail(), mail.messageId(), e.getMessage());
            // 예외를 던지지 않고 계속 진행 (메일 처리는 성공)
        }
    }

    private String formatSlackMessage(String email, MailSummary mail) {
        String receivedTime = mail.receivedDate().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        String formattedSize = formatFileSize(mail.size());

        return String.format("""
                        :envelope: *새 메일이 도착했습니다!*
                        ━━━━━━━━━━━ ━━━━━━━━━━━━━━━━
                        
                        :memo: *제목*
                        %s
                        
                        :bust_in_silhouette: *발신자*
                        `%s
                        
                        :clock10: *수신 시간*
                        %s
                        
                        """,
                mail.fromAddress(),
                mail.subject(),
                receivedTime,
                formattedSize,
                email
        );
    }

    private String formatFileSize(int bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
    }

    private void saveProcessedMail(Long userId, MailSummary mail) {
        ProcessedMail processed = new ProcessedMail(
                userId,
                mail.messageId(),
                mail.subject(),
                mail.fromAddress()
        );
        processedMailRepo.save(processed);
        log.debug("Saved processed mail record: {}", mail.messageId());
    }
}