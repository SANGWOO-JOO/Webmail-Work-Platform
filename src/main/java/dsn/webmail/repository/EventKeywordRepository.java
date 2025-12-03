package dsn.webmail.repository;

import dsn.webmail.entity.EventKeyword;
import dsn.webmail.entity.MailEvent;
import dsn.webmail.entity.TechKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventKeywordRepository extends JpaRepository<EventKeyword, Long> {

    List<EventKeyword> findByEvent(MailEvent event);

    List<EventKeyword> findByEventId(Long eventId);

    List<EventKeyword> findByKeyword(TechKeyword keyword);

    @Query("SELECT ek.keyword FROM EventKeyword ek WHERE ek.event.id = :eventId")
    List<TechKeyword> findKeywordsByEventId(@Param("eventId") Long eventId);

    boolean existsByEventAndKeyword(MailEvent event, TechKeyword keyword);

    @Modifying
    void deleteByEvent(MailEvent event);

    @Query("SELECT ek FROM EventKeyword ek WHERE ek.event.user.id = :userId ORDER BY ek.createdAt DESC")
    List<EventKeyword> findByEventUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT COUNT(ek) FROM EventKeyword ek WHERE ek.keyword.id = :keywordId")
    Long countByKeywordId(@Param("keywordId") Long keywordId);
}
