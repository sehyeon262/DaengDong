package com.e108.be.domain.route.controller;

import com.e108.be.domain.route.dto.request.RouteSelectionRequest;
import com.e108.be.domain.route.dto.response.RouteRecommendResponse;
import com.e108.be.domain.route.entity.WeatherCondition;
import com.e108.be.domain.route.service.RouteSelectionService;
import com.e108.be.domain.route.service.RouteService;
import com.e108.be.global.common.template.ResTemplate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 산책 경로 추천 API
 *
 * GET  /routes/recommend?lat=35.17&lon=129.07&weather=CLEAR  → 경로 추천
 * POST /routes/selection                                      → 경로 선택 기록
 */
@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
@Validated
public class RouteController {

    private final RouteService routeService;
    private final RouteSelectionService routeSelectionService;

    /**
     * 경로 추천 (최대 3개 코스 반환)
     *
     * 로그인한 사용자의 반려견을 자동 조회하여 경로를 추천한다.
     * 날씨 정보를 함께 전달하면 날씨별 선호 경로를 우선 추천한다.
     *
     * @param memberId JWT에서 추출한 회원 ID
     * @param lat      기준 위도 (-90 ~ 90)
     * @param lon      기준 경도 (-180 ~ 180)
     * @param weather  현재 날씨 (선택, CLEAR/CLOUDY/RAIN/SNOW/HOT/COLD)
     */
    @GetMapping("/recommend")
    public ResTemplate<RouteRecommendResponse> recommend(
            @AuthenticationPrincipal Long memberId,
            @RequestParam @Min(-90) @Max(90) double lat,
            @RequestParam @Min(-180) @Max(180) double lon,
            @RequestParam(required = false) WeatherCondition weather) {

        RouteRecommendResponse response = routeService.recommend(memberId, lat, lon, weather);
        return ResTemplate.success(HttpStatus.OK, "경로 추천 성공", response);
    }

    /**
     * 경로 선택 기록
     *
     * 사용자가 추천된 경로 중 하나를 선택했을 때 호출한다.
     * 선택 데이터는 개인화 추천에 활용된다.
     *
     * @param memberId JWT에서 추출한 회원 ID
     * @param request  선택 정보 (경로 유형, 거리, 장소 목록, 날씨)
     */
    @PostMapping("/selection")
    public ResTemplate<Void> logSelection(
            @AuthenticationPrincipal Long memberId,
            @RequestBody @Valid RouteSelectionRequest request) {

        routeSelectionService.logSelection(memberId, request);
        return ResTemplate.success(HttpStatus.OK, "경로 선택 기록 성공");
    }
}
