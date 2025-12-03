package dsn.webmail.repository;

import dsn.webmail.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByEmailAndVerifiedFalseAndExpiresAtAfter(String email, LocalDateTime now);

    Optional<PasswordResetToken> findByEmailAndVerifiedTrueAndExpiresAtAfter(String email, LocalDateTime now);

    void deleteByEmail(String email);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
