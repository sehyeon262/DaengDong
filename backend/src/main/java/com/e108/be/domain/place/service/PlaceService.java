package com.e108.be.domain.place.service;

import com.e108.be.domain.place.dto.request.RegisterPlaceRequest;
import com.e108.be.domain.place.dto.response.NearbyPlaceResponse;
import com.e108.be.domain.place.dto.response.PlaceCategoryResponse;
import com.e108.be.domain.place.dto.response.PlaceDetailResponse;
import com.e108.be.domain.place.dto.response.RegisterPlaceResponse;
import com.e108.be.domain.place.entity.Place;
import com.e108.be.domain.place.entity.PlaceCategory;
import com.e108.be.domain.place.exception.PlaceNotFoundException;
import com.e108.be.domain.place.repository.PlaceCategoryRepository;
import com.e108.be.domain.place.repository.PlaceRepository;
import com.e108.be.global.common.util.S3Service;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private static final double DEFAULT_RADIUS_METERS = 1000.0; // 기본 반경 1km
    private static final int DEFAULT_LIMIT = 20;
    private static final String USER_PROVIDER = "USER";
    private static final String S3_FOLDER = "places";
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final PlaceRepository placeRepository;
    private final PlaceCategoryRepository placeCategoryRepository;
    private final S3Service s3Service;

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

    /**
     * 장소 등록 (사용자 등록, 이미지 포함)
     *
     * provider = "USER", sourceId = UUID (자동 생성)
     * 이미지가 있으면 S3에 업로드 후 URL을 저장
     *
     * @param request 장소 정보
     * @param image   장소 이미지 파일 (선택)
     */
    @Transactional
    public RegisterPlaceResponse registerPlace(RegisterPlaceRequest request, MultipartFile image) {
        String sourceId = UUID.randomUUID().toString();

        // 카테고리 조회 (선택)
        PlaceCategory category = null;
        if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
            category = placeCategoryRepository.findByName(request.getCategoryName())
                    .orElse(null);
        }

        // 이미지 S3 업로드
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = s3Service.uploadShared(image, S3_FOLDER);
        }

        // Point 생성 (X=경도, Y=위도)
        Point location = GEOMETRY_FACTORY.createPoint(
                new Coordinate(request.getLongitude(), request.getLatitude())
        );

        Place place = Place.builder()
                .provider(USER_PROVIDER)
                .sourceId(sourceId)
                .name(request.getName())
                .category(category)
                .location(location)
                .address(request.getAddress())
                .imageUrl(imageUrl)
                .description(request.getMemo())
                .build();

        Place saved = placeRepository.save(place);
        return RegisterPlaceResponse.from(saved);
    }
}
