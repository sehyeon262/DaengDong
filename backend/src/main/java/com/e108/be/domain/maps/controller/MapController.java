package com.e108.be.domain.maps.controller;

import com.e108.be.domain.maps.dto.request.StampRequest;
import com.e108.be.domain.maps.dto.response.FootprintHistoryResponse;
import com.e108.be.domain.maps.dto.response.FootprintMapResponse;
import com.e108.be.domain.maps.service.MapService;
import com.e108.be.global.common.template.ResTemplate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/maps")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    /**
     * M3-01 지도 발자국 조회 (히트맵)
     * GET /api/v1/maps/footprints?dogId={dogId}
     * 강아지의 모든 완료된 산책 경로를 좌표 배열로 반환
     */
    @GetMapping("/footprints")
    public ResponseEntity<ResTemplate<FootprintMapResponse>> getFootprintMap(
            @AuthenticationPrincipal Long memberId,
            @RequestParam Long dogId
    ) {
        FootprintMapResponse response = mapService.getFootprintMap(dogId);
        return ResponseEntity.ok(
                ResTemplate.success(HttpStatus.OK, "발자국 지도 조회 성공", response)
        );
    }

    /**
     * M3-03 발자국 도장 찍기
     * POST /api/v1/maps/stamps
     * FE: 사용자가 장소 50m 이내 접근 → 알림 표시 → 터치 시 호출
     */
    @PostMapping("/stamps")
    public ResponseEntity<ResTemplate<Void>> stamp(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody StampRequest request
    ) {
        mapService.stamp(request);
        return ResponseEntity.ok(
                ResTemplate.success(HttpStatus.OK, "발자국 도장 등록 성공", null)
        );
    }

    /**
     * M3-02 발자국 기록 조회 (과거 산책 목록)
     * GET /api/v1/maps/footprints/history?dogId={dogId}
     * 강아지의 과거 산책 기록 목록 (메타데이터 + 경로 좌표)
     */
    @GetMapping("/footprints/history")
    public ResponseEntity<ResTemplate<List<FootprintHistoryResponse>>> getFootprintHistory(
            @AuthenticationPrincipal Long memberId,
            @RequestParam Long dogId
    ) {
        List<FootprintHistoryResponse> response = mapService.getFootprintHistory(dogId);
        return ResponseEntity.ok(
                ResTemplate.success(HttpStatus.OK, "발자국 기록 조회 성공", response)
        );
    }
}
