package dsn.webmail.repository;

import dsn.webmail.entity.LearningResource;
import dsn.webmail.entity.TechKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {

    List<LearningResource> findByKeyword(TechKeyword keyword);

    List<LearningResource> findByKeywordId(Long keywordId);

    List<LearningResource> findByKeywordIdAndType(Long keywordId, LearningResource.ResourceType type);

    List<LearningResource> findByKeywordIdAndDifficulty(Long keywordId, Integer difficulty);

    List<LearningResource> findByIsRecommendedTrue();

    @Query("SELECT r FROM LearningResource r WHERE r.keyword.id = :keywordId ORDER BY r.type, r.difficulty")
    List<LearningResource> findByKeywordIdOrderByTypeAndDifficulty(@Param("keywordId") Long keywordId);

    @Query("SELECT COUNT(r) FROM LearningResource r WHERE r.keyword.id = :keywordId")
    Long countByKeywordId(@Param("keywordId") Long keywordId);
}
