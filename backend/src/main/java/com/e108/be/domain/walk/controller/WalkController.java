package com.e108.be.domain.walk.controller;

import com.e108.be.domain.walk.dto.request.StartWalkRequest;
import com.e108.be.domain.walk.dto.response.EndWalkResponse;
import com.e108.be.domain.walk.dto.response.StartWalkResponse;
import com.e108.be.domain.walk.dto.response.WalkDurationResponse;
import com.e108.be.domain.walk.dto.response.CaloriesResponse;
import com.e108.be.domain.walk.dto.response.DistanceResponse;
import com.e108.be.domain.walk.service.WalkService;
import com.e108.be.global.common.template.ResTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * [Controller 패키지]
 * - 클라이언트(프론트엔드)의 HTTP 요청을 받는 창구
 * - 요청을 받아서 → Service에 위임
 * - 비즈니스 로직은 여기에 쓰지 않음
 *
 * 요청 흐름: 클라이언트 → Controller → Service → Repository → DB
 * 응답 흐름: DB → Repository → Service → Controller → 클라이언트
 */
@RestController
@RequestMapping("/walks")
@RequiredArgsConstructor
public class WalkController {

    private final WalkService walkService;

    /**
     * W1-01 산책 시작
     * POST /api/v1/walks
     * 누적 거리 조회
     * GET /api/v1/walks/{walkId}/distance
     *
     * 응답:
     * { "code": 200, "message": "거리 조회 성공", "data": {"distanceM": 1234.56, "distanceKm": 1.23} }
     */
    @PostMapping
    public ResTemplate<StartWalkResponse> startWalk(@RequestBody StartWalkRequest request) {
        StartWalkResponse response = walkService.startWalk(request);
        return ResTemplate.success(HttpStatus.OK, "산책이 시작되었습니다.", response);
    }

    @GetMapping("/{walkId}/distance")
    public ResTemplate<DistanceResponse> getDistance(@PathVariable Long walkId) {
        DistanceResponse response = walkService.getDistance(walkId);
        return ResTemplate.success(HttpStatus.OK, "거리 조회 성공", response);
    }

    /**
     * W1-03 산책 시간 조회
     * GET /api/v1/walks/{walkId}/duration
     * 소모 칼로리 조회
     * GET /api/v1/walks/{walkId}/calories
     *
     * 체중 있을 때:
     * { "code": 200, "message": "칼로리 조회 성공", "data": {"calories": 45.6, "requiresWeight": false} }
     *
     * 체중 미입력 시:
     * { "code": 200, "message": "체중을 입력해 주세요", "data": {"requiresWeight": true} }
     */
    @GetMapping("/{walkId}/duration")
    public ResTemplate<WalkDurationResponse> getWalkDuration(@PathVariable Long walkId) {
        WalkDurationResponse response = walkService.getWalkDuration(walkId);
        return ResTemplate.success(HttpStatus.OK, "산책 시간 조회 성공", response);
    }

    /**
     * W1-06 산책 종료
     * POST /api/v1/walks/{walkId}/end
     */
    @PostMapping("/{walkId}/end")
    public ResTemplate<EndWalkResponse> endWalk(@PathVariable Long walkId) {
        EndWalkResponse response = walkService.endWalk(walkId);
        return ResTemplate.success(HttpStatus.OK, "산책이 종료되었습니다.", response);
    }

    @GetMapping("/{walkId}/calories")
    public ResTemplate<CaloriesResponse> getCalories(@PathVariable Long walkId) {
        CaloriesResponse response = walkService.getCalories(walkId);
        if (response.isRequiresWeight()) {
            return ResTemplate.success(HttpStatus.OK, "체중을 입력해 주세요", response);
        }
        return ResTemplate.success(HttpStatus.OK, "칼로리 조회 성공", response);
    }
}
