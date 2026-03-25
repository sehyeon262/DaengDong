package com.e108.be.domain.maps.dto.response;

import com.e108.be.domain.walk.entity.WalkRecord;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * M3-02 GET /api/v1/maps/footprints/history 응답 (단건)
 * 과거 산책 기록 하나의 메타데이터 + 경로 좌표
 */
@Getter
@RequiredArgsConstructor
public class FootprintHistoryResponse {

    private final Long walkId;
    private final LocalDate date;
    private final BigDecimal distanceM;
    private final Integer durationSec;
    private final List<double[]> routeCoordinates;

    public static FootprintHistoryResponse from(WalkRecord record, List<double[]> routeCoordinates) {
        return new FootprintHistoryResponse(
                record.getId(),
                record.getStartTime() != null ? record.getStartTime().toLocalDate() : null,
                record.getTotalDistance(),
                record.getTotalDuration(),
                routeCoordinates
        );
    }
}
