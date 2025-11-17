package dsn.webmail.dto;

import java.time.LocalDateTime;

public record MailSummary(
    String messageId,
    String subject,
    String fromAddress,
    LocalDateTime receivedDate,
    int size
) {}