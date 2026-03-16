package com.e108.be.domain.walk.controller;

import com.e108.be.domain.walk.dto.request.WalkLocationRequest;
import com.e108.be.domain.walk.dto.response.CaloriesResponse;
import com.e108.be.domain.walk.dto.response.DistanceResponse;
import com.e108.be.domain.walk.service.WalkService;
import com.e108.be.global.common.template.ResTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/walks")
@RequiredArgsConstructor
public class WalkController {

    private final WalkService walkService;

    /**
     * GPS 좌표 배치 저장
     * POST /api/v1/walks/{walkId}/locations
     *
     * 요청:
     * {
     *   "locations": [
     *     {"latitude": 37.5665, "longitude": 126.9780, "timestamp": 1234567890000}
     *   ]
     * }
     * 응답:
     * { "code": 200, "message": "위치 저장 성공", "data": {"savedCount": 1} }
     */
    @PostMapping("/{walkId}/locations")
    public ResTemplate<?> saveLocations(
            @PathVariable Long walkId,
            @RequestBody WalkLocationRequest request) {
        int savedCount = walkService.saveLocations(walkId, request);
        return ResTemplate.success(HttpStatus.OK, "위치 저장 성공",
                java.util.Map.of("savedCount", savedCount));
    }

    /**
     * 누적 거리 조회
     * GET /api/v1/walks/{walkId}/distance
     *
     * 응답:
     * { "code": 200, "message": "거리 조회 성공", "data": {"distanceM": 1234.56, "distanceKm": 1.23} }
     */
    @GetMapping("/{walkId}/distance")
    public ResTemplate<DistanceResponse> getDistance(@PathVariable Long walkId) {
        DistanceResponse response = walkService.getDistance(walkId);
        return ResTemplate.success(HttpStatus.OK, "거리 조회 성공", response);
    }

    /**
     * 소모 칼로리 조회
     * GET /api/v1/walks/{walkId}/calories
     *
     * 체중 있을 때:
     * { "code": 200, "message": "칼로리 조회 성공", "data": {"calories": 45.6, "requiresWeight": false} }
     *
     * 체중 미입력 시:
     * { "code": 200, "message": "체중을 입력해 주세요", "data": {"requiresWeight": true} }
     */
    @GetMapping("/{walkId}/calories")
    public ResTemplate<CaloriesResponse> getCalories(@PathVariable Long walkId) {
        CaloriesResponse response = walkService.getCalories(walkId);
        if (response.isRequiresWeight()) {
            return ResTemplate.success(HttpStatus.OK, "체중을 입력해 주세요", response);
        }
        return ResTemplate.success(HttpStatus.OK, "칼로리 조회 성공", response);
    }
}
