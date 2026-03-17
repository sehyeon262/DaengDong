package com.e108.be.domain.place.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * GiST 공간 인덱스 생성
 *
 * 실행 순서:
 *   1) schema.sql → PostGIS 확장 활성화 (Hibernate DDL 이전)
 *   2) Hibernate ddl-auto → places 테이블 생성 (geography 타입 인식 가능)
 *   3) PlaceDbInitializer (Order=1) → GiST 인덱스 생성
 *   4) PlaceCategoryInitializer (Order=2) → 기본 카테고리 데이터 삽입
 *
 * PostGIS 확장은 schema.sql 에서 처리하므로 여기서는 GiST 인덱스만 담당
 * GiST 인덱스는 PostGIS 전용이라 JPA @Index 어노테이션으로 생성 불가
 * location 컬럼이 geography 타입이므로 별도 캐스팅 없이 인덱스 바로 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class PlaceDbInitializer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        createGistIndex();
    }

    private void createGistIndex() {
        try {
            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_places_location ON places USING GIST(location)"
            );
            log.info("[PlaceDb] GiST 공간 인덱스 생성 완료");
        } catch (Exception e) {
            log.warn("[PlaceDb] GiST 인덱스 생성 실패: {}", e.getMessage());
        }
    }
}
