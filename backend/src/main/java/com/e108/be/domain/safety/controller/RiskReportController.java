package com.e108.be.domain.safety.controller;

import com.e108.be.domain.safety.dto.request.CreateRiskReportRequest;
import com.e108.be.domain.safety.dto.response.NearbyRiskReportResponse;
import com.e108.be.domain.safety.dto.response.RiskReportResponse;
import com.e108.be.domain.safety.service.RiskReportService;
import com.e108.be.global.common.template.ResTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/safety")
@RequiredArgsConstructor
public class RiskReportController {

    private final RiskReportService riskReportService;

    /**
     * 위험 구역 신고
     * POST /api/v1/safety/risk-zones
     *
     * Authorization: Bearer {accessToken} 필수 (SecurityConfig에서 인증 요구)
     */
    @PostMapping("/risk-zones")
    @ResponseStatus(HttpStatus.CREATED)
    public ResTemplate<RiskReportResponse> createRiskReport(
            @RequestBody CreateRiskReportRequest request) {
        RiskReportResponse response = riskReportService.createRiskReport(request);
        return ResTemplate.success(HttpStatus.CREATED, "위험 구역 신고가 정상적으로 접수되었습니다.", response);
    }

    /**
     * 주변 위험 구역 조회 (전체 사용자)
     * GET /api/v1/safety/risk-zones?latitude=&longitude=&radiusMeters=
     */
    @GetMapping("/risk-zones")
    public ResTemplate<List<RiskReportResponse>> getRiskZones(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5000.0") double radiusMeters) {
        List<RiskReportResponse> response = riskReportService.getRiskZones(latitude, longitude, radiusMeters);
        return ResTemplate.success(HttpStatus.OK, "주변 위험 구역 조회 성공", response);
    }

    /**
     * 현재 로그인 사용자의 위험 구역 전체 목록 조회
     * GET /api/v1/safety/risk-zones/mine
     *
     * 앱 재실행 후 개인 위험장소 복원 용도
     * Authorization: Bearer {accessToken} 필수
     */
    @GetMapping("/risk-zones/mine")
    public ResTemplate<List<RiskReportResponse>> getMyRiskZones() {
        List<RiskReportResponse> response = riskReportService.getMyRiskZones();
        return ResTemplate.success(HttpStatus.OK, "내 위험 구역 조회 성공", response);
    }

    /**
     * 현재 로그인 사용자의 위험 구역 중 반경 내 목록 조회 (거리 포함)
     * GET /api/v1/safety/risk-zones/nearby?latitude=&longitude=&radiusMeters=
     *
     * 산책 중 근접 알림 입력 용도
     * Authorization: Bearer {accessToken} 필수
     */
    @DeleteMapping("/risk-zones/{riskReportId}")
    public ResTemplate<Void> deleteMyRiskZone(@PathVariable Long riskReportId) {
        riskReportService.deleteMyRiskZone(riskReportId);
        return ResTemplate.success(HttpStatus.OK, "위험 구역이 삭제되었습니다.");
    }

    @GetMapping("/risk-zones/nearby")
    public ResTemplate<List<NearbyRiskReportResponse>> getNearbyMyRiskZones(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "500.0") double radiusMeters) {
        List<NearbyRiskReportResponse> response = riskReportService.getNearbyMyRiskZones(latitude, longitude, radiusMeters);
        return ResTemplate.success(HttpStatus.OK, "내 주변 위험 구역 조회 성공", response);
    }
}
