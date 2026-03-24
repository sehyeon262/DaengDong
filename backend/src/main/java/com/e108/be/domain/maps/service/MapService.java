package com.e108.be.domain.maps.service;

import com.e108.be.domain.maps.dto.request.StampRequest;
import com.e108.be.domain.maps.dto.response.FootprintHistoryResponse;
import com.e108.be.domain.maps.dto.response.FootprintMapResponse;
import com.e108.be.domain.walk.entity.WalkRecord;
import com.e108.be.domain.walk.entity.WalkStatus;
import com.e108.be.domain.walk.repository.WalkRecordRepository;
import com.e108.be.domain.walk.service.FootprintService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapService {

    private final WalkRecordRepository walkRecordRepository;
    private final FootprintService footprintService;

    /**
     * M3-01 GET /api/v1/maps/footprints
     * 강아지의 모든 완료된 산책 경로 좌표 반환 (히트맵용)
     * routes: 산책별 좌표 목록 [ [[lat, lng], ...], [...] ]
     */
    public FootprintMapResponse getFootprintMap(Long dogId) {
        List<WalkRecord> records = walkRecordRepository
                .findAllByDogIdAndWalkStatusOrderByStartTimeDesc(dogId, WalkStatus.COMPLETED);

        List<List<double[]>> routes = records.stream()
                .filter(r -> r.getRouteLine() != null)
                .map(r -> extractCoordinates(r.getRouteLine()))
                .toList();

        return new FootprintMapResponse(routes);
    }

    /**
     * M3-02 GET /api/v1/maps/footprints/history
     * 강아지의 과거 산책 기록 목록 (메타데이터 + 경로)
     */
    public List<FootprintHistoryResponse> getFootprintHistory(Long dogId) {
        List<WalkRecord> records = walkRecordRepository
                .findAllByDogIdAndWalkStatusOrderByStartTimeDesc(dogId, WalkStatus.COMPLETED);

        return records.stream()
                .map(r -> FootprintHistoryResponse.from(r, extractCoordinates(r.getRouteLine())))
                .toList();
    }

    /**
     * M3-03 POST /api/v1/maps/stamps
     * 사용자가 장소 근처(50m)에서 발자국 터치 시 도장 1개 등록
     */
    @Transactional
    public void stamp(StampRequest request) {
        footprintService.registerStamp(request.getWalkId(), request.getDogId(), request.getPlaceId());
    }

    /**
     * JTS LineString → [[lat, lng], ...] 변환
     * JTS 좌표계: X = 경도(longitude), Y = 위도(latitude)
     */
    private List<double[]> extractCoordinates(LineString lineString) {
        if (lineString == null) {
            return List.of();
        }
        return Arrays.stream(lineString.getCoordinates())
                .map(c -> new double[]{c.y, c.x})  // Y=lat, X=lng
                .toList();
    }
}
