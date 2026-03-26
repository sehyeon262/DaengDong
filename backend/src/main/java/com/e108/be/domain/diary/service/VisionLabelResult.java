package com.e108.be.domain.diary.service;

import java.util.List;

/**
 * Google Cloud Vision API 라벨 분석 결과
 *
 * @param labels       감지된 라벨 목록 (예: Dog, Cat, Park, Tree)
 * @param hasDog       강아지가 감지되었는지 여부
 * @param animalLabels 동물 관련 라벨 (Dog, Cat, Bird 등)
 * @param sceneLabels  장면/환경 라벨 (Park, Grass, Sky 등)
 */
public record VisionLabelResult(
        List<String> labels,
        boolean hasDog,
        List<String> animalLabels,
        List<String> sceneLabels
) {
}
