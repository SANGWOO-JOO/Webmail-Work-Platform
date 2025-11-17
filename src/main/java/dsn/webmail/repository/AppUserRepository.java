package dsn.webmail.repository;

import dsn.webmail.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM AppUser u WHERE u.status = 'ACTIVE' AND u.mailPollingEnabled = true AND u.encryptedPop3Password IS NOT NULL AND (u.nextRetryAt IS NULL OR u.nextRetryAt <= :now)")
    List<AppUser> findActiveForPolling(@Param("now") LocalDateTime now);
}