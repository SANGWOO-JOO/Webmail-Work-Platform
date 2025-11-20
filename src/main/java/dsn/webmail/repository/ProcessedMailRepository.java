package dsn.webmail.repository;

import dsn.webmail.entity.MailCategory;
import dsn.webmail.entity.ProcessedMail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ProcessedMailRepository extends JpaRepository<ProcessedMail, Long> {

    boolean existsByUserIdAndMessageId(Long userId, String messageId);

    long countByUserId(Long userId);

    long countByUserIdAndProcessedAtAfter(Long userId, LocalDateTime after);

    // 페이지네이션 조회
    Page<ProcessedMail> findByUserIdOrderByProcessedAtDesc(Long userId, Pageable pageable);

    // 카테고리 필터 + 페이지네이션
    Page<ProcessedMail> findByUserIdAndCategoryOrderByProcessedAtDesc(Long userId, MailCategory category, Pageable pageable);

    // 사용자별 메일 상세 조회
    Optional<ProcessedMail> findByIdAndUserId(Long id, Long userId);

    // 특정 시간 이후 메일 조회
    java.util.List<ProcessedMail> findByUserIdAndProcessedAtAfterOrderByProcessedAtDesc(Long userId, LocalDateTime after);
}