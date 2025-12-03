package dsn.webmail.repository;

import dsn.webmail.entity.UserVisit;
import dsn.webmail.entity.AppUser;
import dsn.webmail.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserVisitRepository extends JpaRepository<UserVisit, Long> {

    List<UserVisit> findByUserOrderByVisitedAtDesc(AppUser user);

    @Query("SELECT uv FROM UserVisit uv WHERE uv.user = :user AND uv.restaurant = :restaurant " +
           "ORDER BY uv.visitedAt DESC")
    List<UserVisit> findByUserAndRestaurantOrderByVisitedAtDesc(@Param("user") AppUser user,
                                                                 @Param("restaurant") Restaurant restaurant);

    @Query("SELECT uv FROM UserVisit uv WHERE uv.restaurant = :restaurant " +
           "ORDER BY uv.visitedAt DESC")
    List<UserVisit> findByRestaurantOrderByVisitedAtDesc(@Param("restaurant") Restaurant restaurant);

    @Query("SELECT uv FROM UserVisit uv WHERE uv.restaurant = :restaurant " +
           "ORDER BY uv.visitedAt DESC LIMIT 5")
    List<UserVisit> findRecentVisitorsByRestaurant(@Param("restaurant") Restaurant restaurant);

    @Query("SELECT uv FROM UserVisit uv WHERE uv.user = :user " +
           "ORDER BY uv.visitedAt DESC LIMIT 10")
    List<UserVisit> findRecentVisitsByUser(@Param("user") AppUser user);

    @Query("SELECT COUNT(uv) > 0 FROM UserVisit uv WHERE uv.user = :user AND uv.restaurant = :restaurant " +
           "AND uv.visitedAt >= :startOfDay AND uv.visitedAt < :endOfDay")
    boolean existsByUserAndRestaurantAndDate(@Param("user") AppUser user,
                                              @Param("restaurant") Restaurant restaurant,
                                              @Param("startOfDay") LocalDateTime startOfDay,
                                              @Param("endOfDay") LocalDateTime endOfDay);
}
