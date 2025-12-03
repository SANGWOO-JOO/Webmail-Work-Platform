package dsn.webmail.service;

import dsn.webmail.dto.MailDtos.MailReplyRequest;
import dsn.webmail.dto.MailDtos.MailReplyResponse;
import dsn.webmail.entity.AppUser;
import dsn.webmail.repository.AppUserRepository;
import dsn.webmail.util.PasswordCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailSendService {

    private final AppUserRepository appUserRepository;
    private final PasswordCipher passwordCipher;

    @Value("${mail.smtp.host}")
    private String smtpHost;

    @Value("${mail.smtp.port}")
    private int smtpPort;

    @Value("${mail.smtp.ssl:true}")
    private boolean smtpSsl;

    @Value("${mail.smtp.system-email:noreply@mts.com}")
    private String systemEmail;

    @Value("${mail.smtp.system-password:}")
    private String systemPassword;

    /**
     * 시스템 메일 발송 (비밀번호 재설정 등)
     */
    public void sendEmail(String to, String subject, String body) throws MessagingException {
        // SMTP 설정
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.auth", "true");

        if (smtpSsl) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", String.valueOf(smtpPort));
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }

        // 세션 생성
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(systemEmail, systemPassword);
            }
        });

        // 메시지 생성
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(systemEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject, "UTF-8");
        message.setText(body, "UTF-8");

        // 메일 발송
        Transport.send(message);

        log.info("시스템 메일 발송 성공: {} -> {}", systemEmail, to);
    }

    public MailReplyResponse sendReply(String userEmail, MailReplyRequest request) {
        try {
            AppUser user = appUserRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userEmail));

            String mailUsername = user.getEmail();
            String mailPassword = passwordCipher.decrypt(user.getEncryptedPop3Password());

            // SMTP 설정
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));
            props.put("mail.smtp.auth", "true");

            if (smtpSsl) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.port", String.valueOf(smtpPort));
            } else {
                props.put("mail.smtp.starttls.enable", "true");
            }

            // 세션 생성
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailUsername, mailPassword);
                }
            });

            // 메시지 생성
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailUsername));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(request.to()));
            message.setSubject(request.subject(), "UTF-8");
            message.setText(request.body(), "UTF-8");

            // 메일 발송
            Transport.send(message);

            log.info("메일 발송 성공: {} -> {}", mailUsername, request.to());
            return new MailReplyResponse(true, "메일이 성공적으로 발송되었습니다.");

        } catch (MessagingException e) {
            log.error("메일 발송 실패: {}", e.getMessage(), e);
            return new MailReplyResponse(false, "메일 발송에 실패했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("메일 발송 중 오류: {}", e.getMessage(), e);
            return new MailReplyResponse(false, "메일 발송 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
