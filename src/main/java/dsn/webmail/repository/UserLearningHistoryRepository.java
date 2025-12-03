package dsn.webmail.repository;

import dsn.webmail.entity.AppUser;
import dsn.webmail.entity.TechKeyword;
import dsn.webmail.entity.UserLearningHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLearningHistoryRepository extends JpaRepository<UserLearningHistory, Long> {

    List<UserLearningHistory> findByUser(AppUser user);

    List<UserLearningHistory> findByUserId(Long userId);

    Optional<UserLearningHistory> findByUserAndKeyword(AppUser user, TechKeyword keyword);

    Optional<UserLearningHistory> findByUserIdAndKeywordId(Long userId, Long keywordId);

    @Query("SELECT h FROM UserLearningHistory h WHERE h.user.id = :userId ORDER BY h.usageCount DESC")
    List<UserLearningHistory> findByUserIdOrderByUsageCountDesc(@Param("userId") Long userId);

    @Query("SELECT h FROM UserLearningHistory h WHERE h.user.id = :userId ORDER BY h.lastUsedAt DESC")
    List<UserLearningHistory> findByUserIdOrderByLastUsedAtDesc(@Param("userId") Long userId);

    @Query("SELECT h.keyword.category, SUM(h.usageCount) FROM UserLearningHistory h WHERE h.user.id = :userId GROUP BY h.keyword.category")
    List<Object[]> findCategoryStatsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(h) FROM UserLearningHistory h WHERE h.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
}
