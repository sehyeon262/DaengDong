package com.e108.be.domain.walk.service;

import com.e108.be.domain.walk.dto.request.StartWalkRequest;
import com.e108.be.domain.walk.dto.response.EndWalkResponse;
import com.e108.be.domain.walk.dto.response.NearbyDogResponse;
import com.e108.be.domain.walk.dto.response.NearbyDogsResponse;
import com.e108.be.domain.walk.dto.response.StartWalkResponse;
import com.e108.be.domain.walk.dto.response.WalkDurationResponse;
import com.e108.be.domain.dog.entity.Dog;
import com.e108.be.domain.dog.repository.DogRepository;
import com.e108.be.domain.walk.dto.response.CaloriesResponse;
import com.e108.be.domain.walk.dto.response.DistanceResponse;
import com.e108.be.domain.walk.entity.WalkRecord;
import com.e108.be.domain.walk.entity.WalkStatus;
import com.e108.be.domain.walk.exception.WalkAlreadyEndedException;
import com.e108.be.domain.walk.exception.WalkAlreadyInProgressException;
import com.e108.be.domain.walk.exception.WalkNotFoundException;
import com.e108.be.domain.walk.repository.WalkRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkService {

    private static final String GPS_KEY_PREFIX = "walk:gps:"; // Redis 키 형식
    private static final double EARTH_RADIUS_M = 6_371_000.0; // 지구 반지름(m)
    private static final double CALORIE_FACTOR = 0.8;         // 칼로리 계산 계수

    private final WalkRecordRepository walkRecordRepository;
    private final DogRepository dogRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public StartWalkResponse startWalk(StartWalkRequest request) {
        // 이미 진행 중인 산책이 있는지 확인
        walkRecordRepository.findByDogIdAndWalkStatus(request.getDogId(), WalkStatus.IN_PROGRESS)
                .ifPresent(w -> { throw new WalkAlreadyInProgressException(); });

        WalkRecord walkRecord = WalkRecord.builder()
                .dogId(request.getDogId())
                .walkStatus(WalkStatus.IN_PROGRESS)
                .startTime(LocalDateTime.now())
                .build();

        WalkRecord saved = walkRecordRepository.save(walkRecord);
        return StartWalkResponse.from(saved);
    }

    /**
     * 누적 거리 조회
     * GET /walks/{walkId}/distance
     *
     * Redis에 쌓인 좌표들을 순서대로 읽어서 인접 좌표 간 Haversine 거리를 합산
     * W1-03 산책 시간 조회
     * GET /api/v1/walks/{walkId}/duration
     */
    public DistanceResponse getDistance(Long walkId) {
        walkRecordRepository.findById(walkId)
                .orElseThrow(WalkNotFoundException::new);

        String redisKey = GPS_KEY_PREFIX + walkId;
        List<String> points = redisTemplate.opsForList().range(redisKey, 0, -1);

        double totalMeters = calculateTotalDistance(points);
        double totalKm = Math.round(totalMeters / 10.0) / 100.0;

        return new DistanceResponse(Math.round(totalMeters * 100.0) / 100.0, totalKm);
    }

    public WalkDurationResponse getWalkDuration(Long walkId) {
        WalkRecord walkRecord = walkRecordRepository.findById(walkId)
                .orElseThrow(WalkNotFoundException::new);
        return WalkDurationResponse.from(walkRecord);
    }

    /**
     * W1-06 산책 종료
     * POST /api/v1/walks/{walkId}/end
     */
    @Transactional
    public EndWalkResponse endWalk(Long walkId) {
        WalkRecord walkRecord = walkRecordRepository.findById(walkId)
                .orElseThrow(WalkNotFoundException::new);

        // 이미 종료된 산책인지 확인
        if (walkRecord.getWalkStatus() != WalkStatus.IN_PROGRESS) {
            throw new WalkAlreadyEndedException();
        }

        // TODO: GPS 연동 완료 후 실제 거리·칼로리를 계산해 전달
        BigDecimal totalDistance = BigDecimal.ZERO;
        BigDecimal calories = BigDecimal.ZERO;

        walkRecord.end(totalDistance, calories);
        return EndWalkResponse.from(walkRecord);
    }

    /**
     * 소모 칼로리 조회
     * GET /walks/{walkId}/calories
     *
     * 칼로리 = 체중(kg) × 거리(km) × 0.8
     * 체중 미입력 시 requiresWeight = true 반환 → FE에서 입력 유도 UI 표시
     */
    public CaloriesResponse getCalories(Long walkId) {
        WalkRecord walk = walkRecordRepository.findById(walkId)
                .orElseThrow(WalkNotFoundException::new);

        Dog dog = dogRepository.findById(walk.getDogId())
                .orElseThrow(() -> new WalkNotFoundException());

        // 체중 미입력 시
        if (dog.getWeight() == null) {
            return new CaloriesResponse(null, true);
        }

        String redisKey = GPS_KEY_PREFIX + walkId;
        List<String> points = redisTemplate.opsForList().range(redisKey, 0, -1);

        double distanceKm = calculateTotalDistance(points) / 1000.0;
        double weightKg = dog.getWeight().doubleValue();
        double calories = BigDecimal.valueOf(weightKg * distanceKm * CALORIE_FACTOR)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        return new CaloriesResponse(calories, false);
    }

    /**
     * S14P21E108-165: 주변 산책 중 강아지 조회
     * GET /api/v1/walks/nearby-dogs
     *
     * 1. IN_PROGRESS 상태인 모든 산책 조회
     * 2. 각 walkId의 Redis에서 마지막 GPS 좌표 추출
     * 3. Haversine으로 거리 계산 → radius 이내만 포함
     * 4. 본인 dogId 제외
     */
    public NearbyDogsResponse getNearbyDogs(double lat, double lon, double radius, Long myDogId) {
        List<WalkRecord> inProgressWalks = walkRecordRepository.findAllByWalkStatus(WalkStatus.IN_PROGRESS);
        List<NearbyDogResponse> nearbyDogs = new ArrayList<>();

        for (WalkRecord walk : inProgressWalks) {
            // 본인 강아지 제외
            if (walk.getDogId().equals(myDogId)) continue;

            // Redis에서 마지막 GPS 좌표 조회
            String redisKey = GPS_KEY_PREFIX + walk.getId();
            String lastPoint = redisTemplate.opsForList().index(redisKey, -1);
            if (lastPoint == null) continue;

            double[] coords = parsePoint(lastPoint);
            double dogLat = coords[0];
            double dogLon = coords[1];

            // 거리 계산 및 필터링
            double distance = haversine(lat, lon, dogLat, dogLon);
            if (distance > radius) continue;

            // 강아지 정보 조회
            Optional<Dog> dogOpt = dogRepository.findById(walk.getDogId());
            if (dogOpt.isEmpty()) continue;
            Dog dog = dogOpt.get();

            nearbyDogs.add(new NearbyDogResponse(
                    dog.getId(),
                    dog.getName(),
                    dog.getBreed(),
                    dog.getProfileImageUrl(),
                    dogLat,
                    dogLon,
                    Math.round(distance * 10.0) / 10.0,
                    walk.getId()
            ));
        }

        // pendingProposals는 172번 (산책 제안) 작업 시 채워짐
        return new NearbyDogsResponse(nearbyDogs, Collections.emptyList());
    }

    /**
     * Haversine 공식으로 좌표 목록의 총 거리(m) 계산
     *
     * 저장 형식 "위도,경도,타임스탬프"에서 위도/경도만 파싱해서 계산
     */
    private double calculateTotalDistance(List<String> points) {
        if (points == null || points.size() < 2) return 0.0;

        double total = 0.0;
        double[] prev = parsePoint(points.get(0));

        for (int i = 1; i < points.size(); i++) {
            double[] curr = parsePoint(points.get(i));
            total += haversine(prev[0], prev[1], curr[0], curr[1]);
            prev = curr;
        }
        return total;
    }

    private double[] parsePoint(String value) {
        String[] parts = value.split(",");
        return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
    }

    /**
     * Haversine 공식: 두 위경도 사이의 거리(m) 계산
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
