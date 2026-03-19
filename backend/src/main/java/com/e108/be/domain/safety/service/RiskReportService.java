package com.e108.be.domain.safety.service;

import com.e108.be.domain.safety.dto.request.CreateRiskReportRequest;
import com.e108.be.domain.safety.dto.response.RiskReportResponse;
import com.e108.be.domain.safety.entity.RiskReport;
import com.e108.be.domain.safety.exception.InvalidCoordinateException;
import com.e108.be.domain.safety.exception.InvalidDescriptionException;
import com.e108.be.domain.safety.exception.UnauthenticatedAccessException;
import com.e108.be.domain.safety.exception.WalkSessionNotFoundException;
import com.e108.be.domain.safety.repository.RiskReportRepository;
import com.e108.be.domain.walk.repository.WalkRecordRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiskReportService {

    // SRID 4326 = WGS84 좌표계 (GPS 표준)
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final RiskReportRepository riskReportRepository;
    private final WalkRecordRepository walkRecordRepository;

    /**
     * 위험 구역 신고 생성
     * POST /api/v1/safety/risk-zones
     *
     * 1. SecurityContext에서 로그인 사용자 ID 추출
     * 2. 좌표 범위 유효성 검증 (서비스 레벨 — ResTemplate 형식 오류 응답 보장)
     * 3. walkSessionId가 있으면 해당 세션 존재 여부 검증
     * 4. JTS Point 생성 후 저장 (Coordinate: x=경도, y=위도)
     */
    @Transactional
    public RiskReportResponse createRiskReport(CreateRiskReportRequest request) {
        Long userId = getCurrentUserId();

        validateDescription(request.getDescription());
        validateCoordinates(request.getLatitude(), request.getLongitude());

        if (request.getWalkSessionId() != null) {
            walkRecordRepository.findById(request.getWalkSessionId())
                    .orElseThrow(WalkSessionNotFoundException::new);
        }

        // PostGIS POINT(경도 위도) 형식: Coordinate(x=longitude, y=latitude)
        // GEOMETRY_FACTORY가 SRID 4326으로 초기화되어 있으므로 setSRID() 불필요
        Point location = GEOMETRY_FACTORY.createPoint(
                new Coordinate(request.getLongitude(), request.getLatitude())
        );

        RiskReport riskReport = RiskReport.builder()
                .userId(userId)
                .walkSessionId(request.getWalkSessionId())
                .location(location)
                .description(request.getDescription())
                .build();

        RiskReport saved = riskReportRepository.save(riskReport);
        return RiskReportResponse.from(saved);
    }

    /**
     * SecurityContext에서 현재 로그인한 사용자 ID를 추출
     *
     * JwtAuthenticationFilter가 UsernamePasswordAuthenticationToken의 principal로
     * Long memberId를 저장하므로 그대로 캐스팅합니다.
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthenticatedAccessException();
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Long)) {
            throw new UnauthenticatedAccessException();
        }
        return (Long) principal;
    }

    /**
     * description null/blank 검증
     */
    private void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new InvalidDescriptionException();
        }
    }

    /**
     * 좌표 범위 유효성 검증
     * - 위도: -90 ~ 90
     * - 경도: -180 ~ 180
     *
     * 서비스 레벨에서 검증해 도메인 예외(InvalidCoordinateException)가
     * ControllerAdvice를 통해 ResTemplate 400으로 반환됩니다.
     */
    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new InvalidCoordinateException("위도와 경도는 필수입니다.");
        }
        if (latitude < -90.0 || latitude > 90.0) {
            throw new InvalidCoordinateException("위도는 -90 이상 90 이하여야 합니다.");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new InvalidCoordinateException("경도는 -180 이상 180 이하여야 합니다.");
        }
    }
}
