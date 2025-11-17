package dsn.webmail.repository;

import dsn.webmail.entity.ProcessedMail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedMailRepository extends JpaRepository<ProcessedMail, Long> {

    boolean existsByUserIdAndMessageId(Long userId, String messageId);
}