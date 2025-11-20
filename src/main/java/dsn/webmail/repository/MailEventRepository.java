package dsn.webmail.repository;

import dsn.webmail.entity.MailEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MailEventRepository extends JpaRepository<MailEvent, Long> {
}
