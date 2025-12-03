package dsn.webmail.repository;

import dsn.webmail.entity.TechKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TechKeywordRepository extends JpaRepository<TechKeyword, Long> {

    Optional<TechKeyword> findByKeyword(String keyword);

    List<TechKeyword> findByCategory(String category);

    List<TechKeyword> findAllByOrderByKeywordAsc();

    boolean existsByKeyword(String keyword);
}
