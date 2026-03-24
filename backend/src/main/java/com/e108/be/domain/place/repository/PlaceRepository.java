package com.e108.be.domain.place.repository;

import com.e108.be.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    /**
     * 반경 내 주변 장소 조회 (거리 오름차순)
     *
     * location 컬럼이 geography 타입이므로 캐스팅 없이 미터 단위 거리 계산
     * ST_DWithin: radiusMeters 반경 이내 필터 + GiST 인덱스 활용
     * ST_Distance: 정확한 거리 반환 (meters)
     *
     * @param lat          기준 위도
     * @param lon          기준 경도
     * @param radiusMeters 검색 반경 (미터)
     * @param limit        최대 반환 개수
     */
    @Query(value = """
        SELECT p.id,
               p.name,
               p.address,
               p.contact,
               p.image_url                                                        AS imageUrl,
               ST_Y(p.location::geometry)                                         AS latitude,
               ST_X(p.location::geometry)                                         AS longitude,
               c.name                                                             AS categoryName,
               c.route_weight                                                     AS routeWeight,
               ST_Distance(
                   p.location,
                   ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
               )                                                                  AS distanceMeters
        FROM places p
        LEFT JOIN place_category c ON p.category_id = c.id
        WHERE p.is_active = true
          AND ST_DWithin(
                  p.location,
                  ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                  :radiusMeters
              )
        ORDER BY distanceMeters
        LIMIT :limit
        """, nativeQuery = true)
    List<NearbyPlaceProjection> findNearby(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusMeters") double radiusMeters,
            @Param("limit") int limit
    );

    /**
     * 카테고리별 반경 내 주변 장소 조회
     */
    @Query(value = """
        SELECT p.id,
               p.name,
               p.address,
               p.contact,
               p.image_url                                                        AS imageUrl,
               ST_Y(p.location::geometry)                                         AS latitude,
               ST_X(p.location::geometry)                                         AS longitude,
               c.name                                                             AS categoryName,
               c.route_weight                                                     AS routeWeight,
               ST_Distance(
                   p.location,
                   ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
               )                                                                  AS distanceMeters
        FROM places p
        LEFT JOIN place_category c ON p.category_id = c.id
        WHERE p.is_active = true
          AND c.name = :categoryName
          AND ST_DWithin(
                  p.location,
                  ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                  :radiusMeters
              )
        ORDER BY distanceMeters
        LIMIT :limit
        """, nativeQuery = true)
    List<NearbyPlaceProjection> findNearbyByCategory(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusMeters") double radiusMeters,
            @Param("categoryName") String categoryName,
            @Param("limit") int limit
    );

    boolean existsByProviderAndSourceId(String provider, String sourceId);

    /**
     * ID 목록으로 장소 조회 (카테고리 함께 FETCH JOIN)
     * N+1 문제 방지를 위해 카테고리를 즉시 로딩한다.
     */
    @Query("SELECT p FROM Place p LEFT JOIN FETCH p.category WHERE p.id IN :ids")
    List<Place> findAllByIdWithCategory(@Param("ids") List<Long> ids);
}
