package dsn.webmail.repository;

import dsn.webmail.entity.ProcessedMail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ProcessedMailRepository extends JpaRepository<ProcessedMail, Long> {
    
    boolean existsByUserIdAndMessageId(Long userId, String messageId);
    
    @Modifying
    @Query("DELETE FROM ProcessedMail p WHERE p.processedAt < :cutoffDate")
    void deleteOldRecords(@Param("cutoffDate") LocalDateTime cutoffDate);
}