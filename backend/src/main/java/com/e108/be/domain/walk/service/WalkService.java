package com.e108.be.domain.walk.service;

import com.e108.be.domain.diary.entity.Diary;
import com.e108.be.domain.diary.repository.DiaryRepository;
import com.e108.be.domain.diary.service.DiaryService;
import com.e108.be.domain.walk.dto.request.*;
import com.e108.be.domain.walk.dto.request.StartWalkRequest;
import com.e108.be.domain.walk.dto.response.*;
import com.e108.be.domain.walk.entity.Feedback;
import com.e108.be.domain.walk.entity.MetDog;
import com.e108.be.domain.walk.entity.ProposalAction;
import com.e108.be.domain.walk.exception.MetDogNotFoundException;
import com.e108.be.domain.walk.exception.ProposalNotFoundException;
import com.e108.be.domain.dog.entity.Dog;
import com.e108.be.domain.dog.repository.DogRepository;
import com.e108.be.domain.walk.dto.response.CaloriesResponse;
import com.e108.be.domain.walk.dto.response.DistanceResponse;
import com.e108.be.domain.walk.dto.response.WalkDetailResponse;
import com.e108.be.global.common.util.S3Service;
import org.springframework.web.multipart.MultipartFile;
import com.e108.be.domain.walk.entity.WalkRecord;
import com.e108.be.domain.walk.entity.WalkStatus;
import com.e108.be.domain.walk.exception.WalkAlreadyEndedException;
import com.e108.be.domain.walk.exception.WalkAlreadyInProgressException;
import com.e108.be.domain.walk.exception.WalkNotFoundException;
import com.e108.be.domain.walk.repository.MetDogRepository;
import com.e108.be.domain.walk.repository.WalkRecordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkService {

    private static final String PROPOSAL_KEY_PREFIX  = "walk:proposals:";
    private static final String ACCEPTED_KEY_PREFIX  = "walk:accepted:";
    private static final double EARTH_RADIUS_M      = 6_371_000.0;
    private static final double CALORIE_FACTOR      = 0.8;

    private final WalkRecordRepository walkRecordRepository;
    private final DogRepository dogRepository;
    private final MetDogRepository metDogRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final DiaryService diaryService;
    private final DiaryRepository diaryRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    @Transactional
    public StartWalkResponse startWalk(StartWalkRequest request) {
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
     * R1-03 자유 산책 시작
     * POST /walks/free-start
     */
    @Transactional
    public StartWalkResponse startFreeWalk(StartWalkRequest request) {
        return startWalk(request);
    }

    /**
     * 누적 거리 조회
     * GET /walks/{walkId}/distance
     */
    public DistanceResponse getDistance(Long walkId) {
        walkRecordRepository.findById(walkId)
                .orElseThrow(WalkNotFoundException::new);

        Double distanceM = walkRecordRepository.getRouteLength(walkId);
        if (distanceM == null) distanceM = 0.0;

        double totalKm = Math.round(distanceM / 10.0) / 100.0;
        return new DistanceResponse(Math.round(distanceM * 100.0) / 100.0, totalKm);
    }

    public WalkDurationResponse getWalkDuration(Long walkId) {
        WalkRecord walkRecord = walkRecordRepository.findById(walkId)
                .orElseThrow(WalkNotFoundException::new);
        return WalkDurationResponse.from(walkRecord);
    }

    /**
     * 산책 종료
     * POST /walks/{walkId}/end
     */
    @Transactional
    public EndWalkResponse endWalk(Long walkId) {
        WalkRecord walkRecord = walkRecordRepository.findById(walkId)
                .orElseThrow(WalkNotFoundException::new);

        if (walkRecord.getWalkStatus() != WalkStatus.IN_PROGRESS) {
            throw new WalkAlreadyEndedException();
        }

        // route_line에서 실제 거리 계산
        Double rawDistance = walkRecordRepository.getRouteLength(walkId);
        double distanceM = rawDistance != null ? rawDistance : 0.0;
        BigDecimal totalDistance = BigDecimal.valueOf(distanceM)
                .setScale(2, RoundingMode.HALF_UP);

        // 칼로리 계산: 체중(kg) × 거리(km) × 0.8
        BigDecimal calories = BigDecimal.ZERO;
        Dog dog = dogRepository.findById(walkRecord.getDogId()).orElse(null);
        if (dog != null && dog.getWeight() != null) {
            double distanceKm = distanceM / 1000.0;
            calories = BigDecimal.valueOf(dog.getWeight().doubleValue() * distanceKm * CALORIE_FACTOR)
                    .setScale(1, RoundingMode.HALF_UP);
        }

        walkRecord.end(totalDistance, calories);

        // 일기 생성 트리거 (300m 이상일 때만)
        diaryService.createDiaryIfEligible(walkId, walkRecord.getDogId(), totalDistance);

        return EndWalkResponse.from(walkRecord);
    }

    /**
     * 소모 칼로리 조회
     * GET /walks/{walkId}/calories
     */
    public CaloriesResponse getCalories(Long walkId) {
        WalkRecord walk = walkRecordRepository.findById(walkId)
                .orElseThrow(WalkNotFoundException::new);

        Dog dog = dogRepository.findById(walk.getDogId())
                .orElseThrow(WalkNotFoundException::new);

        if (dog.getWeight() == null) {
            return new CaloriesResponse(null, true);
        }

        Double distanceM = walkRecordRepository.getRouteLength(walkId);
        double distanceKm = (distanceM != null ? distanceM : 0.0) / 1000.0;
        double weightKg = dog.getWeight().doubleValue();
        double calories = BigDecimal.valueOf(weightKg * distanceKm * CALORIE_FACTOR)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        return new CaloriesResponse(calories, false);
    }

    /**
     * 산책 상세 조회 (산책 데이터 + 강아지 + 일기)
     */
    public WalkDetailResponse getWalkDetail(Long walkId) {
        WalkRecord walk = walkRecordRepository.findById(walkId)
                .orElseThrow(WalkNotFoundException::new);
        Dog dog = dogRepository.findById(walk.getDogId())
                .orElseThrow(WalkNotFoundException::new);
        Diary diary = diaryRepository.findByWalkId(walkId).orElse(null);

        return WalkDetailResponse.from(walk, dog, diary);
    }

    /**
     * 산책 사진 업로드 (S3) 후 URL 목록 저장
     */
    @Transactional
    public List<String> uploadPhotos(Long walkId, List<MultipartFile> files) {
        WalkRecord walkRecord = walkRecordRepository.findById(walkId)
                .orElseThrow(WalkNotFoundException::new);

        List<String> newUrls = files.stream()
                .map(file -> s3Service.upload(file, "walks/" + walkId + "/photos"))
                .toList();

        // 기존 사진에 추가 (덮어쓰지 않음)
        List<String> existing = walkRecord.getPhotoUrls() != null
                ? new ArrayList<>(walkRecord.getPhotoUrls())
                : new ArrayList<>();
        existing.addAll(newUrls);
        walkRecord.updatePhotoUrls(existing);
        return existing;
    }

    /**
     * 사진 삭제
     * DELETE /api/v1/walks/{walkId}/photos
     */
    @Transactional
    public List<String> deletePhoto(Long walkId, String photoUrl) {
        WalkRecord walkRecord = walkRecordRepository.findById(walkId)
                .orElseThrow(WalkNotFoundException::new);

        // S3에서 실제 파일 삭제
        s3Service.delete(photoUrl);

        // DB에서 URL 제거
        List<String> updated = new ArrayList<>(
                walkRecord.getPhotoUrls() != null ? walkRecord.getPhotoUrls() : Collections.emptyList()
        );
        updated.remove(photoUrl);
        walkRecord.updatePhotoUrls(updated);
        return updated;
    }

    /**
     * S14P21E108-165: 주변 산책 중 강아지 조회
     * GET /api/v1/walks/nearby-dogs
     *
     * myWalkRecordId 전달 시 내게 온 pendingProposals도 함께 반환
     */
    public NearbyDogsResponse getNearbyDogs(double lat, double lon, double radius,
                                             Long myDogId, Long myWalkRecordId) {
        List<WalkRecord> inProgressWalks = walkRecordRepository.findAllByWalkStatus(WalkStatus.IN_PROGRESS);
        log.info("[nearbyDogs] IN_PROGRESS {}개 조회됨. myDogId={}, lat={}, lon={}, radius={}",
                inProgressWalks.size(), myDogId, lat, lon, radius);
        List<NearbyDogResponse> nearbyDogs = new ArrayList<>();

        for (WalkRecord walk : inProgressWalks) {
            if (walk.getDogId().equals(myDogId)) {
                log.info("[nearbyDogs] walkId={} dogId={} → 내 강아지, skip", walk.getId(), walk.getDogId());
                continue;
            }

            List<Object[]> lastPointList = walkRecordRepository.getLastPoint(walk.getId());
            if (lastPointList == null || lastPointList.isEmpty()) {
                log.info("[nearbyDogs] walkId={} dogId={} → route_line NULL (GPS 미저장), skip",
                        walk.getId(), walk.getDogId());
                continue;
            }
            Object[] lastPoint = lastPointList.get(0);
            if (lastPoint == null || lastPoint.length < 2) {
                log.warn("[nearbyDogs] walkId={} → lastPoint 형식 이상: length={}",
                        walk.getId(), lastPoint == null ? "null" : lastPoint.length);
                continue;
            }

            double dogLat = ((Number) lastPoint[0]).doubleValue();
            double dogLon = ((Number) lastPoint[1]).doubleValue();

            double distance = haversine(lat, lon, dogLat, dogLon);
            log.info("[nearbyDogs] walkId={} dogId={} → dogLat={}, dogLon={}, distance={}m",
                    walk.getId(), walk.getDogId(), dogLat, dogLon, distance);
            if (distance > radius) {
                log.info("[nearbyDogs] walkId={} → 거리 초과({}m > {}m), skip", walk.getId(), distance, radius);
                continue;
            }

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
        log.info("[nearbyDogs] 최종 결과: {}마리", nearbyDogs.size());

        // 내게 온 pending proposals 조회
        List<PendingProposalResponse> pendingProposals = new ArrayList<>();
        if (myWalkRecordId != null) {
            String hashKey = PROPOSAL_KEY_PREFIX + myWalkRecordId;
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(hashKey);
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                try {
                    String proposalId = entry.getKey().toString();
                    Map<String, Object> data = objectMapper.readValue(
                            entry.getValue().toString(),
                            new TypeReference<Map<String, Object>>() {}
                    );
                    Long fromWalkRecordId = Long.valueOf(data.get("fromWalkRecordId").toString());

                    WalkRecord fromRecord = walkRecordRepository.findById(fromWalkRecordId).orElse(null);
                    if (fromRecord == null) continue;

                    dogRepository.findById(fromRecord.getDogId()).ifPresent(dog ->
                            pendingProposals.add(new PendingProposalResponse(
                                    proposalId,
                                    fromWalkRecordId,
                                    dog.getId(),
                                    dog.getName(),
                                    dog.getBreed(),
                                    dog.getProfileImageUrl()
                            ))
                    );
                } catch (Exception e) {
                    log.warn("[nearbyDogs] pendingProposal Redis 파싱 오류: {}", e.getMessage());
                }
            }
        }

        // 내가 보낸 제안이 수락됐는지 조회
        List<AcceptedProposalResponse> acceptedProposals = new ArrayList<>();
        if (myWalkRecordId != null) {
            String acceptKey = ACCEPTED_KEY_PREFIX + myWalkRecordId;
            Map<Object, Object> acceptedEntries = redisTemplate.opsForHash().entries(acceptKey);
            for (Map.Entry<Object, Object> entry : acceptedEntries.entrySet()) {
                try {
                    String proposalId = entry.getKey().toString();
                    Map<String, Object> data = objectMapper.readValue(
                            entry.getValue().toString(),
                            new TypeReference<Map<String, Object>>() {}
                    );
                    Long dogId = Long.valueOf(data.get("dogId").toString());
                    String name = data.get("name").toString();
                    String breed = data.get("breed").toString();
                    String profileImageUrl = data.containsKey("profileImageUrl")
                            ? data.get("profileImageUrl").toString() : null;
                    acceptedProposals.add(new AcceptedProposalResponse(proposalId, dogId, name, breed, profileImageUrl));
                    // 읽었으면 삭제 (1회성 알림)
                    redisTemplate.opsForHash().delete(acceptKey, proposalId);
                } catch (Exception e) {
                    log.warn("[nearbyDogs] acceptedProposal Redis 파싱 오류: {}", e.getMessage());
                }
            }
        }

        return new NearbyDogsResponse(nearbyDogs, pendingProposals, acceptedProposals);
    }

    /**
     * S14P21E108-172: 산책 제안 전송
     * POST /walks/proposals
     */
    @Transactional
    public ProposalResponse sendProposal(ProposalRequest request) {
        walkRecordRepository.findById(request.getFromWalkRecordId())
                .orElseThrow(WalkNotFoundException::new);
        walkRecordRepository.findById(request.getToWalkRecordId())
                .orElseThrow(WalkNotFoundException::new);

        String proposalId = UUID.randomUUID().toString();
        String hashKey = PROPOSAL_KEY_PREFIX + request.getToWalkRecordId();

        try {
            String value = objectMapper.writeValueAsString(Map.of(
                    "fromWalkRecordId", request.getFromWalkRecordId(),
                    "timestamp", Instant.now().toString()
            ));
            redisTemplate.opsForHash().put(hashKey, proposalId, value);
            redisTemplate.expire(hashKey, 4, TimeUnit.HOURS);
        } catch (Exception e) {
            throw new RuntimeException("제안 저장 실패", e);
        }

        return new ProposalResponse(proposalId);
    }

    /**
     * S14P21E108-172: 산책 제안 수락/거절
     * PATCH /walks/proposals/{proposalId}
     */
    @Transactional
    public void respondToProposal(String proposalId, ProposalRespondRequest request) {
        String hashKey = PROPOSAL_KEY_PREFIX + request.getMyWalkRecordId();
        Object raw = redisTemplate.opsForHash().get(hashKey, proposalId);
        if (raw == null) {
            throw new ProposalNotFoundException();
        }

        if (ProposalAction.ACCEPT == request.getAction()) {
            try {
                Map<String, Object> data = objectMapper.readValue(
                        raw.toString(),
                        new TypeReference<Map<String, Object>>() {}
                );
                Long fromWalkRecordId = Long.valueOf(data.get("fromWalkRecordId").toString());

                WalkRecord myRecord = walkRecordRepository.findById(request.getMyWalkRecordId())
                        .orElseThrow(WalkNotFoundException::new);
                WalkRecord fromRecord = walkRecordRepository.findById(fromWalkRecordId)
                        .orElseThrow(WalkNotFoundException::new);

                // 양방향 met_dogs upsert
                upsertMetDog(myRecord, fromRecord.getDogId());
                upsertMetDog(fromRecord, myRecord.getDogId());

                // 제안자(fromWalkRecordId)에게 수락 알림 저장 (30분 TTL)
                Dog myDog = dogRepository.findById(myRecord.getDogId()).orElse(null);
                if (myDog != null) {
                    try {
                        String acceptKey = ACCEPTED_KEY_PREFIX + fromWalkRecordId;
                        String acceptValue = objectMapper.writeValueAsString(Map.of(
                                "dogId", myDog.getId(),
                                "name", myDog.getName(),
                                "breed", myDog.getBreed(),
                                "profileImageUrl", myDog.getProfileImageUrl() != null ? myDog.getProfileImageUrl() : ""
                        ));
                        redisTemplate.opsForHash().put(acceptKey, proposalId, acceptValue);
                        redisTemplate.expire(acceptKey, 30, TimeUnit.MINUTES);
                    } catch (Exception e) {
                        log.warn("[respondToProposal] 수락 알림 Redis 저장 오류: {}", e.getMessage());
                    }
                }
            } catch (ProposalNotFoundException | WalkNotFoundException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("제안 수락 처리 실패", e);
            }
        }

        // ACCEPT / REJECT 모두 Redis에서 삭제
        redisTemplate.opsForHash().delete(hashKey, proposalId);
    }

    /**
     * S14P21E108-172: 산책 종료 시 2m 만남 일괄 기록 (프론트에서 목록 전달)
     * POST /walks/{walkRecordId}/encounters
     */
    @Transactional
    public void recordEncounters(Long walkRecordId, EncounterRequest request) {
        WalkRecord myRecord = walkRecordRepository.findById(walkRecordId)
                .orElseThrow(WalkNotFoundException::new);

        for (EncounterRequest.EncounterItem item : request.getEncounters()) {
            WalkRecord targetRecord = walkRecordRepository.findById(item.getTargetWalkRecordId())
                    .orElseThrow(WalkNotFoundException::new);

            // 양방향 met_dogs upsert
            upsertMetDog(myRecord, targetRecord.getDogId());
            upsertMetDog(targetRecord, myRecord.getDogId());
        }
    }

    /**
     * S14P21E108-172: 피드백 설정/수정
     * PATCH /walks/met-dogs/feedback
     */
    @Transactional
    public void updateFeedback(FeedbackRequest request) {
        WalkRecord myRecord = walkRecordRepository.findById(request.getMyWalkRecordId())
                .orElseThrow(WalkNotFoundException::new);

        MetDog metDog = metDogRepository
                .findBySourceDogIdAndTargetDogId(myRecord.getDogId(), request.getTargetDogId())
                .orElseThrow(MetDogNotFoundException::new);

        metDog.updateFeedback(request.getFeedback());
    }

    /**
     * met_dogs upsert 헬퍼.
     * (sourceDog, targetDog) 조합이 이미 있으면 latest_walk_record 업데이트,
     * 없으면 feedback="보통"으로 신규 insert.
     */
    private void upsertMetDog(WalkRecord sourceRecord, Long targetDogId) {
        metDogRepository.findBySourceDogIdAndTargetDogId(sourceRecord.getDogId(), targetDogId)
                .ifPresentOrElse(
                        existing -> existing.updateLatestWalkRecord(sourceRecord),
                        () -> metDogRepository.save(MetDog.builder()
                                .latestWalkRecord(sourceRecord)
                                .targetDogId(targetDogId)
                                .feedback(Feedback.보통)
                                .build())
                );
    }

    /**
     * 만난 친구 목록 조회
     * GET /walks/met-dogs?dogId=
     */
    public List<MetDogResponse> getMetDogs(Long dogId) {
        List<MetDog> metDogs = metDogRepository.findAllBySourceDogId(dogId);

        // N+1 방지: targetDogId 일괄 조회
        List<Long> targetDogIds = metDogs.stream()
                .map(MetDog::getTargetDogId)
                .toList();
        Map<Long, Dog> dogMap = dogRepository.findAllById(targetDogIds).stream()
                .collect(java.util.stream.Collectors.toMap(Dog::getId, d -> d));

        return metDogs.stream()
                .filter(md -> dogMap.containsKey(md.getTargetDogId()))
                .map(md -> MetDogResponse.from(md, dogMap.get(md.getTargetDogId())))
                .toList();
    }

    // ────────────── 내부 유틸 ──────────────

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
