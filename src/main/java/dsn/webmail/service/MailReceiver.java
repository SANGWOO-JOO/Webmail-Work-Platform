package dsn.webmail.service;

import dsn.webmail.config.MailPop3Properties;
import dsn.webmail.dto.MailSummary;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
        
        return new MailSummary(messageId, subject, fromAddress, receivedDate, size);
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
}