package com.e108.be.domain.walk.controller;

import com.e108.be.domain.walk.dto.request.WalkLocationRequest;
import com.e108.be.domain.walk.service.WalkLocationService;
import com.e108.be.global.common.template.ResTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * GPS 좌표 관련 API
 * POST /walks/{walkId}/locations
 */
@RestController
@RequestMapping("/walks")
@RequiredArgsConstructor
public class WalkLocationController {

    private final WalkLocationService walkLocationService;

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
        int savedCount = walkLocationService.saveLocations(walkId, request);
        return ResTemplate.success(HttpStatus.OK, "위치 저장 성공", Map.of("savedCount", savedCount));
    }
}
