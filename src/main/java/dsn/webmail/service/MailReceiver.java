package dsn.webmail.service;

import dsn.webmail.config.MailPop3Properties;
import dsn.webmail.dto.MailSummary;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Service
@Slf4j
public class MailReceiver {
    private final MailPop3Properties properties;

    public MailReceiver(MailPop3Properties properties) {
        this.properties = properties;
    }

    public List<MailSummary> fetchRecent(String username, String password) throws MessagingException {
        log.debug("Fetching recent mails for user: {}", username);
        
        Properties props = new Properties();
        props.put("mail.store.protocol", "pop3s");
        props.put("mail.pop3s.host", properties.host());
        props.put("mail.pop3s.port", properties.port());
        props.put("mail.pop3s.ssl.enable", properties.ssl());
        props.put("mail.pop3s.connectiontimeout", properties.connectionTimeoutMs());
        props.put("mail.pop3s.timeout", properties.readTimeoutMs());
        
        Session session = Session.getDefaultInstance(props);
        Store store = null;
        Folder folder = null;
        
        try {
            store = session.getStore("pop3s");
            store.connect(properties.host(), username, password);
            
            folder = store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            
            int messageCount = folder.getMessageCount();
            if (messageCount == 0) {
                log.debug("No messages found for user: {}", username);
                return List.of();
            }
            
            int start = Math.max(1, messageCount - properties.maxFetch() + 1);
            Message[] messages = folder.getMessages(start, messageCount);
            
            List<MailSummary> summaries = new ArrayList<>();
            for (Message message : messages) {
                try {
                    MailSummary summary = convertToSummary(message);
                    summaries.add(summary);
                } catch (Exception e) {
                    log.warn("Failed to parse message for user {}: {}", username, e.getMessage());
                }
            }

            log.debug("Fetched {} messages for user: {}", summaries.size(), username);
            return summaries;
            
        } finally {
            if (folder != null && folder.isOpen()) {
                folder.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
        }
    }

    private MailSummary convertToSummary(Message message) throws MessagingException {
        String messageId = extractMessageId(message);
        String subject = message.getSubject() != null ? message.getSubject() : "(제목 없음)";
        String fromAddress = extractFromAddress(message);
        LocalDateTime receivedDate = extractReceivedDate(message);
        int size = message.getSize();
        String content = extractContent(message);

        return new MailSummary(messageId, subject, fromAddress, receivedDate, size, content);
    }

    private String extractMessageId(Message message) throws MessagingException {
        String[] messageIds = message.getHeader("Message-ID");
        if (messageIds != null && messageIds.length > 0 && messageIds[0] != null) {
            return messageIds[0];
        }
        
        return generateFallbackMessageId(message);
    }

    private String generateFallbackMessageId(Message message) throws MessagingException {
        try {
            String subject = message.getSubject() != null ? message.getSubject() : "";
            String from = extractFromAddress(message);
            Date receivedDate = message.getReceivedDate() != null ? message.getReceivedDate() : new Date();
            int size = message.getSize();
            
            String input = subject + from + receivedDate.getTime() + size;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return "fallback-" + hexString.toString().substring(0, 16);
        } catch (Exception e) {
            log.warn("Failed to generate fallback message ID: {}", e.getMessage());
            return "fallback-" + System.currentTimeMillis();
        }
    }

    private String extractFromAddress(Message message) throws MessagingException {
        Address[] from = message.getFrom();
        if (from != null && from.length > 0) {
            if (from[0] instanceof InternetAddress internetAddress) {
                return internetAddress.getAddress();
            }
            return from[0].toString();
        }
        return "(발신자 알 수 없음)";
    }

    private LocalDateTime extractReceivedDate(Message message) throws MessagingException {
        Date receivedDate = message.getReceivedDate();
        if (receivedDate != null) {
            return receivedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }

        Date sentDate = message.getSentDate();
        if (sentDate != null) {
            return sentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }

        return LocalDateTime.now();
    }

    /**
     * 이메일 본문 내용을 추출합니다.
     * 단순 텍스트 메일과 Multipart 메일 모두 처리합니다.
     */
    private String extractContent(Message message) {
        try {
            Object content = message.getContent();

            if (content instanceof String) {
                return (String) content;
            } else if (content instanceof Multipart) {
                return extractTextFromMultipart((Multipart) content);
            }

            return "";
        } catch (MessagingException | IOException e) {
            log.warn("Failed to extract content from message: {}", e.getMessage());
            return "(본문을 읽을 수 없습니다)";
        }
    }

    /**
     * Multipart 메일에서 텍스트 본문을 추출합니다.
     * text/plain 우선, 없으면 text/html 사용 (중복 방지)
     * 중첩된 Multipart도 재귀적으로 처리합니다.
     */
    private String extractTextFromMultipart(Multipart multipart) throws MessagingException, IOException {
        String plainText = null;
        String htmlText = null;

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            if (bodyPart.isMimeType("text/plain")) {
                // text/plain 파트 저장
                plainText = bodyPart.getContent().toString();
            } else if (bodyPart.isMimeType("text/html")) {
                // text/html 파트 저장
                htmlText = bodyPart.getContent().toString();
            } else if (bodyPart.getContent() instanceof Multipart) {
                // 중첩된 Multipart 재귀 처리
                String nested = extractTextFromMultipart((Multipart) bodyPart.getContent());
                if (!nested.isEmpty()) {
                    return nested;
                }
            }
        }

        // text/plain 우선, 없으면 text/html 사용
        if (plainText != null) {
            return decodeHtmlEntities(plainText);
        } else if (htmlText != null) {
            return stripHtmlTags(htmlText);
        }

        return "";
    }

    /**
     * HTML 엔티티만 디코딩합니다 (태그 제거 없이).
     * text/plain 파트에 포함된 &nbsp;, &lt; 같은 엔티티를 일반 문자로 변환합니다.
     */
    private String decodeHtmlEntities(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        try {
            // Jsoup.parse()를 사용하여 HTML 엔티티 디코딩
            // 태그가 없는 순수 텍스트이므로 태그 제거는 발생하지 않음
            return Jsoup.parse(text).text();
        } catch (Exception e) {
            log.warn("Failed to decode HTML entities: {}", e.getMessage());
            return text;
        }
    }

    /**
     * HTML 태그를 제거하고 순수 텍스트만 추출합니다.
     * 줄바꿈을 보존하면서 HTML 엔티티를 디코딩합니다.
     */
    private String stripHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        try {
            // 1. 블록 레벨 요소를 줄바꿈으로 변환
            String processed = html
                .replaceAll("(?i)<br\\s*/?>", "\n")           // <br> → 줄바꿈
                .replaceAll("(?i)</p>", "\n")                 // </p> → 줄바꿈
                .replaceAll("(?i)</div>", "\n")               // </div> → 줄바꿈
                .replaceAll("(?i)</h[1-6]>", "\n")            // </h1-6> → 줄바꿈
                .replaceAll("(?i)</li>", "\n")                // </li> → 줄바꿈
                .replaceAll("(?i)</tr>", "\n");               // </tr> → 줄바꿈

            // 2. Jsoup으로 HTML 태그 제거 및 엔티티 디코딩
            String text = Jsoup.parse(processed).text();

            // 3. 연속된 공백/줄바꿈 정리
            return text
                .replaceAll("[ \\t]+", " ")                   // 연속된 공백을 하나로
                .replaceAll("\n{3,}", "\n\n")                 // 3개 이상 줄바꿈을 2개로
                .trim();                                       // 앞뒤 공백 제거

        } catch (Exception e) {
            log.warn("Failed to strip HTML tags: {}", e.getMessage());
            // 파싱 실패 시 원본 반환
            return html;
        }
    }
}