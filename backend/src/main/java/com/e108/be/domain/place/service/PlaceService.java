package com.e108.be.domain.place.service;

import com.e108.be.domain.place.dto.response.NearbyPlaceResponse;
import com.e108.be.domain.place.dto.response.PlaceCategoryResponse;
import com.e108.be.domain.place.dto.response.PlaceDetailResponse;
import com.e108.be.domain.place.exception.PlaceNotFoundException;
import com.e108.be.domain.place.repository.PlaceCategoryRepository;
import com.e108.be.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private static final double DEFAULT_RADIUS_METERS = 1000.0; // 기본 반경 1km
    private static final int DEFAULT_LIMIT = 20;

    private final PlaceRepository placeRepository;
    private final PlaceCategoryRepository placeCategoryRepository;

    /**
     * 반경 내 주변 장소 조회
     *
     * @param lat          기준 위도
     * @param lon          기준 경도
     * @param radiusMeters 검색 반경 (미터), null이면 기본값 1km
     * @param limit        최대 개수, null이면 기본값 20
     * @param categoryName 카테고리 필터 (null이면 전체)
     */
    public List<NearbyPlaceResponse> getNearbyPlaces(
            double lat, double lon,
            Double radiusMeters, Integer limit,
            String categoryName) {

        double radius = radiusMeters != null ? radiusMeters : DEFAULT_RADIUS_METERS;
        int count = limit != null ? limit : DEFAULT_LIMIT;

        if (categoryName != null && !categoryName.isBlank()) {
            return placeRepository.findNearbyByCategory(lat, lon, radius, categoryName, count)
                    .stream()
                    .map(NearbyPlaceResponse::from)
                    .toList();
        }

        return placeRepository.findNearby(lat, lon, radius, count)
                .stream()
                .map(NearbyPlaceResponse::from)
                .toList();
    }

    /**
     * 장소 상세 조회
     */
    public PlaceDetailResponse getPlace(Long placeId) {
        return placeRepository.findById(placeId)
                .map(PlaceDetailResponse::from)
                .orElseThrow(PlaceNotFoundException::new);
    }

    /**
     * 전체 카테고리 목록 조회
     */
    public List<PlaceCategoryResponse> getCategories() {
        return placeCategoryRepository.findAll()
                .stream()
                .map(PlaceCategoryResponse::from)
                .toList();
    }
}
