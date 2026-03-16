package com.e108.be.domain.place.init;

import com.e108.be.domain.place.entity.PlaceCategory;
import com.e108.be.domain.place.repository.PlaceCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 앱 기동 완료 후 place_category 기본 데이터를 JPA로 삽입
 * - 이미 존재하는 카테고리는 건너뜀 (idempotent)
 * - 새 카테고리 추가 시 이 목록에 추가하면 됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class PlaceCategoryInitializer {

    private final PlaceCategoryRepository placeCategoryRepository;

    // name, routeWeight
    private static final List<Object[]> DEFAULT_CATEGORIES = List.of(
        new Object[]{"동물병원",     0},
        new Object[]{"동물약국",     2},
        new Object[]{"미용",         2},
        new Object[]{"반려동물용품", 3},
        new Object[]{"위탁관리",     1},
        new Object[]{"카페",         5},
        new Object[]{"식당",         5},
        new Object[]{"박물관",       5},
        new Object[]{"미술관",       5},
        new Object[]{"문예회관",     5},
        new Object[]{"여행지",       8},
        new Object[]{"펜션",         3},
        new Object[]{"호텔",         3}
    );

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        Set<String> existing = placeCategoryRepository.findAll().stream()
                .map(PlaceCategory::getName)
                .collect(Collectors.toSet());

        List<PlaceCategory> toInsert = DEFAULT_CATEGORIES.stream()
                .filter(row -> !existing.contains((String) row[0]))
                .map(row -> PlaceCategory.builder()
                        .name((String) row[0])
                        .routeWeight((Integer) row[1])
                        .build())
                .toList();

        if (!toInsert.isEmpty()) {
            placeCategoryRepository.saveAll(toInsert);
            log.info("[PlaceCategory] 기본 카테고리 {}개 삽입 완료", toInsert.size());
        }
    }
}
