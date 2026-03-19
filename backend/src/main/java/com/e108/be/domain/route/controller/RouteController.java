package com.e108.be.domain.route.controller;

import com.e108.be.domain.route.dto.response.RouteRecommendResponse;
import com.e108.be.domain.route.service.RouteService;
import com.e108.be.global.common.template.ResTemplate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 산책 경로 추천 API
 *
 * GET /routes/recommend?lat=35.17&lon=129.07&dogId=1
 */
@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
@Validated
public class RouteController {

    private final RouteService routeService;

    /**
     * 경로 추천 (최대 3개 코스 반환)
     *
     * @param lat   기준 위도 (-90 ~ 90)
     * @param lon   기준 경도 (-180 ~ 180)
     * @param dogId 반려견 ID
     */
    @GetMapping("/recommend")
    public ResTemplate<RouteRecommendResponse> recommend(
            @RequestParam @Min(-90) @Max(90) double lat,
            @RequestParam @Min(-180) @Max(180) double lon,
            @RequestParam Long dogId) {

        RouteRecommendResponse response = routeService.recommend(lat, lon, dogId);
        return ResTemplate.success(HttpStatus.OK, "경로 추천 성공", response);
    }
}
