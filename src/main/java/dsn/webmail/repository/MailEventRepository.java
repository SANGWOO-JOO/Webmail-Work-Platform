package dsn.webmail.repository;

import dsn.webmail.entity.MailEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MailEventRepository extends JpaRepository<MailEvent, Long> {

    // 사용자의 특정 월 일정 조회
    @Query("SELECT e FROM MailEvent e WHERE e.user.id = :userId AND e.dateTime LIKE :yearMonth%")
    List<MailEvent> findByUserIdAndYearMonth(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);

    // 사용자의 모든 일정 조회
    List<MailEvent> findByUserId(Long userId);

    // 사용자의 특정 일정 조회
    Optional<MailEvent> findByIdAndUserId(Long id, Long userId);
}
