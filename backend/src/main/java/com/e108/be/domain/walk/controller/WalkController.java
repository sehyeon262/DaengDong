package com.e108.be.domain.walk.controller;

import com.e108.be.domain.walk.dto.request.StartWalkRequest;
import com.e108.be.domain.walk.dto.response.StartWalkResponse;
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
     */
    @PostMapping
    public ResTemplate<StartWalkResponse> startWalk(@RequestBody StartWalkRequest request) {
        StartWalkResponse response = walkService.startWalk(request);
        return ResTemplate.success(HttpStatus.OK, "산책이 시작되었습니다.", response);
    }
}
