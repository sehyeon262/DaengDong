package com.e108.be.domain.place.controller;

import com.e108.be.domain.place.dto.request.RegisterPlaceRequest;
import com.e108.be.domain.place.dto.response.NearbyPlaceResponse;
import com.e108.be.domain.place.dto.response.PlaceCategoryResponse;
import com.e108.be.domain.place.dto.response.PlaceDetailResponse;
import com.e108.be.domain.place.dto.response.RegisterPlaceResponse;
import com.e108.be.domain.place.service.PlaceService;
import com.e108.be.global.common.template.ResTemplate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 장소 API
 *
 * POST /places                   장소 등록
 * GET  /places/nearby?lat=&lon=  주변 장소 조회
 * GET  /places/{id}              장소 상세 조회
 * GET  /places/categories        카테고리 목록 조회
 */
@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
@Validated
public class PlaceController {

    private final PlaceService placeService;

    /**
     * 장소 등록 (이미지 포함)
     *
     * multipart/form-data로 장소 정보 + 이미지 파일을 함께 전송
     *
     * @param request 장소 정보 (name, latitude, longitude, categoryName, address, memo)
     * @param image   장소 이미지 파일 (선택)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResTemplate<RegisterPlaceResponse> registerPlace(
            @Valid @ModelAttribute RegisterPlaceRequest request,
            @RequestPart(required = false) MultipartFile image) {

        RegisterPlaceResponse response = placeService.registerPlace(request, image);
        return ResTemplate.success(HttpStatus.CREATED, "장소 등록 성공", response);
    }

    /**
     * 주변 장소 조회
     *
     * @param lat      기준 위도 (-90 ~ 90)
     * @param lon      기준 경도 (-180 ~ 180)
     * @param radius   검색 반경 미터 (기본값 1000m, 최대 5000m)
     * @param limit    최대 반환 개수 (기본값 20, 최대 100)
     * @param category 카테고리 이름 필터 (선택, 예: 카페, 동물병원)
     */
    @GetMapping("/nearby")
    public ResTemplate<List<NearbyPlaceResponse>> getNearbyPlaces(
            @RequestParam @Min(-90) @Max(90) double lat,
            @RequestParam @Min(-180) @Max(180) double lon,
            @RequestParam(required = false) @Min(100) @Max(5000) Double radius,
            @RequestParam(required = false) @Min(1) @Max(100) Integer limit,
            @RequestParam(required = false) String category) {

        List<NearbyPlaceResponse> places = placeService.getNearbyPlaces(lat, lon, radius, limit, category);
        return ResTemplate.success(HttpStatus.OK, "주변 장소 조회 성공", places);
    }

    /**
     * 장소 상세 조회
     */
    @GetMapping("/{placeId}")
    public ResTemplate<PlaceDetailResponse> getPlace(@PathVariable Long placeId) {
        PlaceDetailResponse place = placeService.getPlace(placeId);
        return ResTemplate.success(HttpStatus.OK, "장소 조회 성공", place);
    }

    /**
     * 카테고리 목록 조회
     */
    @GetMapping("/categories")
    public ResTemplate<List<PlaceCategoryResponse>> getCategories() {
        List<PlaceCategoryResponse> categories = placeService.getCategories();
        return ResTemplate.success(HttpStatus.OK, "카테고리 조회 성공", categories);
    }
}
