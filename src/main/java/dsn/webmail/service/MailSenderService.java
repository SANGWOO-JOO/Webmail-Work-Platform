package dsn.webmail.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@Slf4j
public class MailSenderService {

    public void sendVerificationCode(String email, String password, String code) {
        try {
            // 사용자의 계정으로 SMTP 설정
            JavaMailSender dynamicSender = createMailSender(email, password);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(email);
            message.setTo(email);
            message.setSubject("[Mail To Slack] 이메일 인증 코드");
            message.setText("인증 코드: " + code + "\n10분 내 입력해주세요.\n\n보안을 위해 이 코드를 다른 사람과 공유하지 마세요.");
            
            dynamicSender.send(message);
            log.info("Verification code sent to: {} using their own account", email);
        } catch (Exception e) {
            log.error("Failed to send verification code to {}: {}", email, e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage());
        }
    }
    
    private JavaMailSender createMailSender(String email, String password) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("smtp.whoisworks.com");
        sender.setPort(587);
        sender.setUsername(email);
        sender.setPassword(password);
        
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.debug", "true"); // 디버깅용
        
        return sender;
    }
    
    // 기존 메서드와 호환성 유지
    public void sendVerificationMail(String fromEmail, String fromPassword, String toEmail, String code) {
        sendVerificationCode(fromEmail, fromPassword, code);
    }
}