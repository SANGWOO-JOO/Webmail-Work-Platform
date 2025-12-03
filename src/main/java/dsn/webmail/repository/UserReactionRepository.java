package dsn.webmail.repository;

import dsn.webmail.entity.UserReaction;
import dsn.webmail.entity.UserReaction.ReactionType;
import dsn.webmail.entity.AppUser;
import dsn.webmail.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserReactionRepository extends JpaRepository<UserReaction, Long> {

    Optional<UserReaction> findByUserAndRestaurant(AppUser user, Restaurant restaurant);

    void deleteByUserAndRestaurant(AppUser user, Restaurant restaurant);

    long countByRestaurantAndType(Restaurant restaurant, ReactionType type);

    @Query("SELECT COALESCE(ur.user.name, ur.user.email) FROM UserReaction ur WHERE ur.restaurant = :restaurant AND ur.type = :type")
    List<String> findUserNamesByRestaurantAndType(@Param("restaurant") Restaurant restaurant,
                                                   @Param("type") ReactionType type);

    @Query("SELECT ur FROM UserReaction ur WHERE ur.restaurant = :restaurant AND ur.type = 'LIKE'")
    List<UserReaction> findLikesByRestaurant(@Param("restaurant") Restaurant restaurant);

    @Query("SELECT ur FROM UserReaction ur WHERE ur.restaurant = :restaurant AND ur.type = 'DISLIKE'")
    List<UserReaction> findDislikesByRestaurant(@Param("restaurant") Restaurant restaurant);
}
