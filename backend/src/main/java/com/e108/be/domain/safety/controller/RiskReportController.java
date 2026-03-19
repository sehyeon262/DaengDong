package com.e108.be.domain.safety.controller;

import com.e108.be.domain.safety.dto.request.CreateRiskReportRequest;
import com.e108.be.domain.safety.dto.response.RiskReportResponse;
import com.e108.be.domain.safety.service.RiskReportService;
import com.e108.be.global.common.template.ResTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
}
