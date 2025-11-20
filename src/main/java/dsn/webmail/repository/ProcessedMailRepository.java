package dsn.webmail.repository;

import dsn.webmail.entity.ProcessedMail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ProcessedMailRepository extends JpaRepository<ProcessedMail, Long> {

    boolean existsByUserIdAndMessageId(Long userId, String messageId);

    long countByUserId(Long userId);

    long countByUserIdAndProcessedAtAfter(Long userId, LocalDateTime after);
}