package dsn.webmail.repository;

import dsn.webmail.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, String> {

    List<Restaurant> findByIsActiveTrue();

    List<Restaurant> findByIsActiveTrueOrderByDistanceAsc();

    List<Restaurant> findByIsActiveTrueAndDistanceLessThanEqualOrderByDistanceAsc(Integer distance);

    List<Restaurant> findByIsActiveTrueAndCategoryContainingOrderByDistanceAsc(String category);

    @Query("SELECT r FROM Restaurant r WHERE r.isActive = true " +
           "AND (:category IS NULL OR r.category LIKE %:category%) " +
           "AND (:maxDistance IS NULL OR r.distance <= :maxDistance) " +
           "ORDER BY r.distance ASC")
    List<Restaurant> findByFilters(@Param("category") String category,
                                   @Param("maxDistance") Integer maxDistance);

    @Query("SELECT r FROM Restaurant r WHERE r.isActive = true " +
           "AND (:category IS NULL OR r.category LIKE %:category%) " +
           "AND (:maxDistance IS NULL OR r.distance <= :maxDistance) " +
           "ORDER BY r.rating DESC NULLS LAST")
    List<Restaurant> findByFiltersOrderByRating(@Param("category") String category,
                                                @Param("maxDistance") Integer maxDistance);

    // 카테고리 분석용 쿼리
    @Query("SELECT DISTINCT r.category FROM Restaurant r WHERE r.isActive = true ORDER BY r.category")
    List<String> findDistinctCategories();

    // 카테고리 레벨별 필터 쿼리
    @Query("SELECT r FROM Restaurant r WHERE r.isActive = true " +
           "AND (:level1 IS NULL OR r.categoryLevel1 = :level1) " +
           "AND (:level2 IS NULL OR r.categoryLevel2 = :level2) " +
           "AND (:level3 IS NULL OR r.categoryLevel3 = :level3) " +
           "AND (:maxDistance IS NULL OR r.distance <= :maxDistance) " +
           "ORDER BY r.distance ASC")
    List<Restaurant> findByCategoryLevels(@Param("level1") String level1,
                                          @Param("level2") String level2,
                                          @Param("level3") String level3,
                                          @Param("maxDistance") Integer maxDistance);

    @Query("SELECT r FROM Restaurant r WHERE r.isActive = true " +
           "AND (:level1 IS NULL OR r.categoryLevel1 = :level1) " +
           "AND (:level2 IS NULL OR r.categoryLevel2 = :level2) " +
           "AND (:level3 IS NULL OR r.categoryLevel3 = :level3) " +
           "AND (:maxDistance IS NULL OR r.distance <= :maxDistance) " +
           "ORDER BY r.rating DESC NULLS LAST")
    List<Restaurant> findByCategoryLevelsOrderByRating(@Param("level1") String level1,
                                                       @Param("level2") String level2,
                                                       @Param("level3") String level3,
                                                       @Param("maxDistance") Integer maxDistance);

    // 카테고리 레벨별 distinct 조회 (계층적 필터 UI용)
    @Query("SELECT DISTINCT r.categoryLevel1 FROM Restaurant r WHERE r.isActive = true AND r.categoryLevel1 IS NOT NULL ORDER BY r.categoryLevel1")
    List<String> findDistinctLevel1();

    @Query("SELECT DISTINCT r.categoryLevel2 FROM Restaurant r WHERE r.isActive = true AND r.categoryLevel1 = :level1 AND r.categoryLevel2 IS NOT NULL ORDER BY r.categoryLevel2")
    List<String> findDistinctLevel2ByLevel1(@Param("level1") String level1);

    @Query("SELECT DISTINCT r.categoryLevel3 FROM Restaurant r WHERE r.isActive = true AND r.categoryLevel1 = :level1 AND r.categoryLevel2 = :level2 AND r.categoryLevel3 IS NOT NULL ORDER BY r.categoryLevel3")
    List<String> findDistinctLevel3ByLevel1AndLevel2(@Param("level1") String level1, @Param("level2") String level2);
}
