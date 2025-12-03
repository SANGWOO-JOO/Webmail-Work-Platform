package dsn.webmail.repository;

import dsn.webmail.entity.UserFavoriteRestaurant;
import dsn.webmail.entity.AppUser;
import dsn.webmail.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFavoriteRestaurantRepository extends JpaRepository<UserFavoriteRestaurant, Long> {

    List<UserFavoriteRestaurant> findByUserOrderByCreatedAtDesc(AppUser user);

    Optional<UserFavoriteRestaurant> findByUserAndRestaurant(AppUser user, Restaurant restaurant);

    boolean existsByUserAndRestaurant(AppUser user, Restaurant restaurant);

    void deleteByUserAndRestaurant(AppUser user, Restaurant restaurant);

    @Query("SELECT uf.restaurant.id FROM UserFavoriteRestaurant uf WHERE uf.user = :user")
    List<String> findRestaurantIdsByUser(@Param("user") AppUser user);

    @Query("SELECT COALESCE(uf.user.name, uf.user.email) FROM UserFavoriteRestaurant uf WHERE uf.restaurant = :restaurant")
    List<String> findUserNamesByRestaurant(@Param("restaurant") Restaurant restaurant);

    long countByRestaurant(Restaurant restaurant);
}
