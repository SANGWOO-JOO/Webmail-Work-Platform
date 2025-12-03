package dsn.webmail.repository;

import dsn.webmail.entity.AppUser;
import dsn.webmail.entity.LearningResource;
import dsn.webmail.entity.UserResourceBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserResourceBookmarkRepository extends JpaRepository<UserResourceBookmark, Long> {

    List<UserResourceBookmark> findByUser(AppUser user);

    List<UserResourceBookmark> findByUserId(Long userId);

    List<UserResourceBookmark> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserResourceBookmark> findByUserAndResource(AppUser user, LearningResource resource);

    Optional<UserResourceBookmark> findByUserIdAndResourceId(Long userId, Long resourceId);

    boolean existsByUserIdAndResourceId(Long userId, Long resourceId);

    void deleteByUserIdAndResourceId(Long userId, Long resourceId);

    Long countByUserId(Long userId);
}
