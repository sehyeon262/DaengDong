package com.e108.be.domain.walk.service;

import com.e108.be.domain.badge.dto.response.BadgeResponse;
import com.e108.be.domain.badge.service.BadgeService;
import com.e108.be.domain.chat.service.ChatService;
import com.e108.be.domain.diary.entity.Diary;
import com.e108.be.domain.diary.repository.DiaryRepository;
import com.e108.be.domain.diary.service.DiaryService;
import com.e108.be.domain.route.dto.request.RouteSelectionRequest;
import com.e108.be.domain.route.service.RouteSelectionService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkService {

    private static final String PROPOSAL_KEY_PREFIX  = "walk:proposals:";
    private static final String ACCEPTED_KEY_PREFIX  = "walk:accepted:";
    private static final String REJECTED_KEY_PREFIX  = "walk:rejected:";
    private static final double EARTH_RADIUS_M      = 6_371_000.0;
    private static final double CALORIE_FACTOR      = 0.8;
    private static final double DEVIATION_BUFFER_M  = 50.0;  // 이탈 판정 거리 (미터)

    private final WalkRecordRepository walkRecordRepository;
    private final DogRepository dogRepository;
    private final MetDogRepository metDogRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final DiaryService diaryService;
    private final DiaryRepository diaryRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;
    private final BadgeService badgeService;
    private final RouteSelectionService routeSelectionService;
    private final ChatService chatService;

    /**
     * W1-01 산책 시작
     * POST /api/v1/walks
     *
     * 경로 추천 기반 산책: selectedType이 있으면 경로 선택 로그도 함께 기록
     * 자유 산책: selectedType이 null이면 산책만 시작
     */
    @Transactional
    public StartWalkResponse startWalk(Long memberId, StartWalkRequest request) {
        // 기존 IN_PROGRESS 산책이 있으면 자동 강제 종료 (stuck 방지)
        walkRecordRepository.findByDogIdAndWalkStatus(request.getDogId(), WalkStatus.IN_PROGRESS)
                .ifPresent(w -> {
                    log.warn("[startWalk] 기존 IN_PROGRESS 산책 강제 종료: walkId={}", w.getId());
                    w.end(w.getTotalDistance() != null ? w.getTotalDistance() : BigDecimal.ZERO,
                          w.getCalories() != null ? w.getCalories() : BigDecimal.ZERO);
                });

        WalkRecord walkRecord = WalkRecord.builder()
                .dogId(request.getDogId())
                .walkStatus(WalkStatus.IN_PROGRESS)
                .startTime(LocalDateTime.now())
                .routeType(request.getSelectedType())  // null이면 자유 산책
                .build();

        WalkRecord saved = walkRecordRepository.save(walkRecord);

        // 경로 추천 기반 산책이면 선택 로그 기록 (개인화 추천에 활용)
        if (request.hasRouteSelection() && memberId != null) {
            try {
                RouteSelectionRequest selectionRequest = RouteSelectionRequest.of(
                        request.getSelectedType(),
                        request.getSelectedDistanceM() != null ? request.getSelectedDistanceM() : 0,
                        request.getPlaceIds(),
                        request.getWeatherCondition(),
                        request.getTemperature()
                );
                routeSelectionService.logSelection(memberId, selectionRequest);
                log.debug("경로 선택 로그 기록: memberId={}, type={}", memberId, request.getSelectedType());
            } catch (Exception e) {
                // 선택 로그 실패가 산책 시작을 막으면 안 됨
                log.warn("경로 선택 로그 기록 실패 (산책은 정상 시작): memberId={}", memberId, e);
            }

            // 추천 경로 좌표 저장 (이탈률 계산용)
            saveRecommendedRoute(saved.getId(), request.getRecommendedPath());
        }

        return StartWalkResponse.from(saved);
    }

    /**
     * R1-03 자유 산책 시작
     * POST /walks/free-start
     *
     * @deprecated startWalk()로 통합됨. selectedType을 null로 보내면 자유 산책.
     */
    @Deprecated
    @Transactional
    public StartWalkResponse startFreeWalk(Long memberId, StartWalkRequest request) {
        return startWalk(memberId, request);
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

        // 이탈률 계산 (추천 경로가 있는 경우만)
        calculateAndSaveDeviationRate(walkRecord);

        // 일기 생성 트리거 — 트랜잭션 커밋 후 비동기 실행
        final Long dogIdForDiary = walkRecord.getDogId();
        diaryService.createDiaryIfEligible(walkId, dogIdForDiary, totalDistance);

        // 배지 체크 — 실패해도 산책 종료는 정상 처리
        List<BadgeResponse> newBadges = new ArrayList<>();
        try {
            if (dog != null) {
                newBadges = badgeService.checkWalkEndBadges(dog.getUser().getId());
            }
        } catch (Exception e) {
            log.error("[endWalk] 배지 체크 중 에러 발생 (산책 종료는 정상 처리됨): walkId={}", walkId, e);
        }

        // 채팅방 종료 — 실패해도 산책 종료는 정상 처리
        try {
            chatService.closeRoomByWalkRecordId(walkId);
        } catch (Exception e) {
            log.error("[endWalk] 채팅방 종료 중 에러 발생 (산책 종료는 정상 처리됨): walkId={}", walkId, e);
        }

        return EndWalkResponse.from(walkRecord, newBadges);
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
     *
     * 성능 개선: PostGIS ST_DWithin 기반 spatial query로 반경 필터링 및 거리 계산을
     * DB에서 처리하여 전체 IN_PROGRESS 스캔 및 N+1 문제 해결
     */
    public NearbyDogsResponse getNearbyDogs(double lat, double lon, double radius,
                                             Long myDogId, Long myWalkRecordId) {
        // 1. PostGIS spatial query로 반경 내 진행 중 산책 세션 조회
        //    반환: [walkRecordId, dogId, lat, lon, distanceM]
        List<Object[]> nearbyWalks = walkRecordRepository.findNearbyInProgressWalks(lat, lon, radius, myDogId);
        log.debug("[nearbyDogs] spatial query 결과: {}건. myDogId={}, lat={}, lon={}, radius={}",
                nearbyWalks.size(), myDogId, lat, lon, radius);

        if (nearbyWalks.isEmpty()) {
            return new NearbyDogsResponse(new ArrayList<>(), getPendingProposals(myWalkRecordId), getAcceptedProposals(myWalkRecordId), getRejectedProposals(myWalkRecordId));
        }

        // 2. 비선호(싫어요) 강아지 목록 한 번에 조회 → Set으로 변환
        Set<Long> dislikedDogIds = myDogId != null
                ? new HashSet<>(metDogRepository.findDislikedTargetDogIds(myDogId))
                : Collections.emptySet();

        // 3. 내 피드백 목록 일괄 로드 (N+1 방지)
        Map<Long, String> myFeedbackMap = new HashMap<>();
        if (myDogId != null) {
            metDogRepository.findAllBySourceDogId(myDogId)
                    .forEach(md -> myFeedbackMap.put(md.getTargetDogId(), md.getFeedback().name()));
        }

        // 4. dogId 목록 추출 후 Dog 정보 일괄 조회 (N+1 방지)
        List<Long> dogIds = nearbyWalks.stream()
                .map(row -> ((Number) row[1]).longValue())
                .toList();
        Map<Long, Dog> dogMap = dogRepository.findAllById(dogIds).stream()
                .collect(java.util.stream.Collectors.toMap(Dog::getId, d -> d));

        // 5. 응답 생성
        List<NearbyDogResponse> nearbyDogs = new ArrayList<>();
        for (Object[] row : nearbyWalks) {
            Long walkRecordId = ((Number) row[0]).longValue();
            Long dogId = ((Number) row[1]).longValue();
            double dogLat = ((Number) row[2]).doubleValue();
            double dogLon = ((Number) row[3]).doubleValue();
            double distanceM = ((Number) row[4]).doubleValue();

            Dog dog = dogMap.get(dogId);
            if (dog == null) {
                log.warn("[nearbyDogs] dogId={} 에 해당하는 강아지 정보 없음", dogId);
                continue;
            }

            String feedback = myFeedbackMap.get(dogId);
            boolean avoidAlertCandidate = dislikedDogIds.contains(dogId);

            nearbyDogs.add(new NearbyDogResponse(
                    dog.getId(),
                    dog.getName(),
                    dog.getBreed(),
                    dog.getProfileImageUrl(),
                    dogLat,
                    dogLon,
                    Math.round(distanceM * 10.0) / 10.0,
                    walkRecordId,
                    feedback,
                    avoidAlertCandidate
            ));
        }
        log.debug("[nearbyDogs] 최종 결과: {}마리", nearbyDogs.size());

        return new NearbyDogsResponse(nearbyDogs, getPendingProposals(myWalkRecordId), getAcceptedProposals(myWalkRecordId), getRejectedProposals(myWalkRecordId));
    }

    /**
     * 내게 온 pending proposals 조회 (Redis)
     */
    private List<PendingProposalResponse> getPendingProposals(Long myWalkRecordId) {
        List<PendingProposalResponse> pendingProposals = new ArrayList<>();
        if (myWalkRecordId == null) {
            return pendingProposals;
        }

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
        return pendingProposals;
    }

    /**
     * 내가 보낸 제안이 수락됐는지 조회 (Redis)
     */
    private List<AcceptedProposalResponse> getAcceptedProposals(Long myWalkRecordId) {
        List<AcceptedProposalResponse> acceptedProposals = new ArrayList<>();
        if (myWalkRecordId == null) {
            return acceptedProposals;
        }

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
                Long chatRoomId = data.containsKey("chatRoomId") && data.get("chatRoomId") != null
                        ? Long.valueOf(data.get("chatRoomId").toString()) : null;
                acceptedProposals.add(new AcceptedProposalResponse(proposalId, dogId, name, breed, profileImageUrl, chatRoomId));
                // 읽었으면 삭제 (1회성 알림)
                redisTemplate.opsForHash().delete(acceptKey, proposalId);
            } catch (Exception e) {
                log.warn("[nearbyDogs] acceptedProposal Redis 파싱 오류: {}", e.getMessage());
            }
        }
        return acceptedProposals;
    }

    /**
     * 내가 보낸 제안이 거절됐는지 조회 (Redis)
     */
    private List<RejectedProposalResponse> getRejectedProposals(Long myWalkRecordId) {
        List<RejectedProposalResponse> rejectedProposals = new ArrayList<>();
        if (myWalkRecordId == null) {
            return rejectedProposals;
        }

        String rejectKey = REJECTED_KEY_PREFIX + myWalkRecordId;
        Map<Object, Object> rejectedEntries = redisTemplate.opsForHash().entries(rejectKey);
        for (Map.Entry<Object, Object> entry : rejectedEntries.entrySet()) {
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
                rejectedProposals.add(new RejectedProposalResponse(proposalId, dogId, name, breed, profileImageUrl));
                // 읽었으면 삭제 (1회성 알림)
                redisTemplate.opsForHash().delete(rejectKey, proposalId);
            } catch (Exception e) {
                log.warn("[nearbyDogs] rejectedProposal Redis 파싱 오류: {}", e.getMessage());
            }
        }
        return rejectedProposals;
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

                // 배지 체크 (양쪽 사용자)
                Dog myDogForBadge = dogRepository.findById(myRecord.getDogId()).orElse(null);
                Dog fromDogForBadge = dogRepository.findById(fromRecord.getDogId()).orElse(null);
                if (myDogForBadge != null) badgeService.checkMeetBadges(myDogForBadge.getUser().getId());
                if (fromDogForBadge != null) badgeService.checkMeetBadges(fromDogForBadge.getUser().getId());

                // 채팅방 생성
                Long chatRoomId = null;
                if (myDogForBadge != null && fromDogForBadge != null) {
                    try {
                        Long fromMemberId = fromDogForBadge.getUser().getId();
                        Long toMemberId = myDogForBadge.getUser().getId();
                        chatRoomId = chatService.createChatRoom(fromMemberId, toMemberId, fromWalkRecordId, request.getMyWalkRecordId());
                        log.debug("[respondToProposal] 채팅방 생성 완료: chatRoomId={}", chatRoomId);
                    } catch (Exception e) {
                        log.warn("[respondToProposal] 채팅방 생성 오류: {}", e.getMessage());
                    }
                }

                // 제안자(fromWalkRecordId)에게 수락 알림 저장 (30분 TTL)
                Dog myDog = dogRepository.findById(myRecord.getDogId()).orElse(null);
                if (myDog != null) {
                    try {
                        String acceptKey = ACCEPTED_KEY_PREFIX + fromWalkRecordId;
                        Map<String, Object> acceptData = new java.util.HashMap<>();
                        acceptData.put("dogId", myDog.getId());
                        acceptData.put("name", myDog.getName());
                        acceptData.put("breed", myDog.getBreed());
                        acceptData.put("profileImageUrl", myDog.getProfileImageUrl() != null ? myDog.getProfileImageUrl() : "");
                        if (chatRoomId != null) {
                            acceptData.put("chatRoomId", chatRoomId);
                        }
                        String acceptValue = objectMapper.writeValueAsString(acceptData);
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

        // REJECT: 제안자에게 거절 알림 저장 (30분 TTL)
        if (ProposalAction.REJECT == request.getAction()) {
            try {
                Map<String, Object> data = objectMapper.readValue(
                        raw.toString(),
                        new TypeReference<Map<String, Object>>() {}
                );
                Long fromWalkRecordId = Long.valueOf(data.get("fromWalkRecordId").toString());

                WalkRecord myRecord = walkRecordRepository.findById(request.getMyWalkRecordId()).orElse(null);
                if (myRecord != null) {
                    Dog myDog = dogRepository.findById(myRecord.getDogId()).orElse(null);
                    if (myDog != null) {
                        String rejectKey = REJECTED_KEY_PREFIX + fromWalkRecordId;
                        String rejectValue = objectMapper.writeValueAsString(Map.of(
                                "dogId", myDog.getId(),
                                "name", myDog.getName(),
                                "breed", myDog.getBreed(),
                                "profileImageUrl", myDog.getProfileImageUrl() != null ? myDog.getProfileImageUrl() : ""
                        ));
                        redisTemplate.opsForHash().put(rejectKey, proposalId, rejectValue);
                        redisTemplate.expire(rejectKey, 30, TimeUnit.MINUTES);
                    }
                }
            } catch (Exception e) {
                log.warn("[respondToProposal] 거절 알림 Redis 저장 오류: {}", e.getMessage());
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

        // 배지 체크
        Dog myDog = dogRepository.findById(myRecord.getDogId()).orElse(null);
        if (myDog != null) {
            badgeService.checkMeetBadges(myDog.getUser().getId());
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

        // met_dogs 레코드가 없으면 보통으로 자동 생성 후 피드백 갱신 (upsert)
        MetDog metDog = metDogRepository
                .findBySourceDogIdAndTargetDogId(myRecord.getDogId(), request.getTargetDogId())
                .orElseGet(() -> metDogRepository.save(MetDog.builder()
                        .latestWalkRecord(myRecord)
                        .targetDogId(request.getTargetDogId())
                        .feedback(Feedback.보통)
                        .build()));

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

    /**
     * 추천 경로 좌표를 WKT LINESTRING으로 변환하여 DB에 저장
     * 좌표 형식: [[lat, lon], [lat, lon], ...]
     */
    private void saveRecommendedRoute(Long walkId, List<List<Double>> recommendedPath) {
        if (recommendedPath == null || recommendedPath.size() < 2) {
            return;
        }
        try {
            StringBuilder wkt = new StringBuilder("LINESTRING(");
            for (int i = 0; i < recommendedPath.size(); i++) {
                List<Double> point = recommendedPath.get(i);
                if (point.size() < 2) continue;
                if (i > 0) wkt.append(", ");
                // WKT는 lon lat 순서
                wkt.append(point.get(1)).append(" ").append(point.get(0));
            }
            wkt.append(")");
            walkRecordRepository.saveRecommendedRoute(walkId, wkt.toString());
            log.debug("추천 경로 저장 완료: walkId={}, points={}", walkId, recommendedPath.size());
        } catch (Exception e) {
            log.warn("추천 경로 저장 실패 (산책은 정상 진행): walkId={}", walkId, e);
        }
    }

    /**
     * 이탈률 계산: 실제 경로의 각 포인트 중 추천 경로에서 50m 이상 벗어난 비율
     * recommended_route가 있고, route_line이 있을 때만 계산
     */
    private void calculateAndSaveDeviationRate(WalkRecord walkRecord) {
        if (walkRecord.getRouteType() == null) {
            return; // 자유 산책은 이탈률 계산 불필요
        }
        try {
            Double deviationRate = walkRecordRepository.calculateDeviationRate(
                    walkRecord.getId(), DEVIATION_BUFFER_M);
            if (deviationRate != null) {
                walkRecord.updateDeviationRate(
                        BigDecimal.valueOf(deviationRate).setScale(2, RoundingMode.HALF_UP));
                log.info("이탈률 계산 완료: walkId={}, deviationRate={}%",
                        walkRecord.getId(), deviationRate);
            }
        } catch (Exception e) {
            log.warn("이탈률 계산 실패 (산책 종료는 정상 처리): walkId={}", walkRecord.getId(), e);
        }
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
