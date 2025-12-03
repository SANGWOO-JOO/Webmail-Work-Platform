package dsn.webmail.repository;

import dsn.webmail.entity.MailKeyword;
import dsn.webmail.entity.ProcessedMail;
import dsn.webmail.entity.TechKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MailKeywordRepository extends JpaRepository<MailKeyword, Long> {

    List<MailKeyword> findByMail(ProcessedMail mail);

    List<MailKeyword> findByMailId(Long mailId);

    List<MailKeyword> findByKeyword(TechKeyword keyword);

    List<MailKeyword> findByKeywordId(Long keywordId);

    @Query("SELECT mk FROM MailKeyword mk WHERE mk.keyword.id = :keywordId ORDER BY mk.createdAt DESC")
    List<MailKeyword> findByKeywordIdOrderByCreatedAtDesc(@Param("keywordId") Long keywordId);

    @Query("SELECT mk.keyword FROM MailKeyword mk WHERE mk.mail.id = :mailId")
    List<TechKeyword> findKeywordsByMailId(@Param("mailId") Long mailId);

    @Query("SELECT COUNT(mk) FROM MailKeyword mk WHERE mk.keyword.id = :keywordId")
    Long countByKeywordId(@Param("keywordId") Long keywordId);

    @Query("SELECT mk.keyword.id, COUNT(mk) FROM MailKeyword mk WHERE mk.mail.user.id = :userId GROUP BY mk.keyword.id ORDER BY COUNT(mk) DESC")
    List<Object[]> findKeywordUsageByUserId(@Param("userId") Long userId);

    @Query("SELECT mk FROM MailKeyword mk WHERE mk.mail.user.id = :userId ORDER BY mk.createdAt DESC")
    List<MailKeyword> findByMailUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    boolean existsByMailAndKeyword(ProcessedMail mail, TechKeyword keyword);
}
