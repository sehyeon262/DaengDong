package com.e108.be.domain.badge.service;

import com.e108.be.domain.auth.entity.User;
import com.e108.be.domain.auth.repository.UserRepository;
import com.e108.be.domain.badge.dto.response.BadgeProgressResponse;
import com.e108.be.domain.badge.dto.response.BadgeResponse;
import com.e108.be.domain.badge.dto.response.UserBadgeResponse;
import com.e108.be.domain.badge.entity.Badge;
import com.e108.be.domain.badge.entity.UserBadge;
import com.e108.be.domain.badge.repository.BadgeRepository;
import com.e108.be.domain.badge.repository.UserBadgeRepository;
import com.e108.be.domain.dog.entity.Dog;
import com.e108.be.domain.dog.repository.DogRepository;
import com.e108.be.domain.safety.repository.RiskReportRepository;
import com.e108.be.domain.walk.repository.FootprintRepository;
import com.e108.be.domain.walk.repository.MetDogRepository;
import com.e108.be.domain.walk.repository.WalkRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BadgeService {

    // 배지명 상수
    private static final String PLACE_5 = "발자국 콩콩";
    private static final String PLACE_20 = "세상은 넓다개";
    private static final String NEW_COURSE_5 = "길 위의 모험가";
    private static final String RISK_REPORT_3 = "우리 동네 지킴이";
    private static final String PHOTO_DIARY_10 = "추억 수집가";
    private static final String PHOTO_10 = "오늘도 찰칵";
    private static final String PROFILE_REGISTERED = "우리집 스타";
    private static final String FIRST_MEET = "우리는 이제 친구";
    private static final String MEET_5_DOGS = "우리 동네 인싸";

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserRepository userRepository;
    private final DogRepository dogRepository;
    private final WalkRecordRepository walkRecordRepository;
    private final FootprintRepository footprintRepository;
    private final RiskReportRepository riskReportRepository;
    private final MetDogRepository metDogRepository;

    /**
     * 산책 종료 시 호출 — 배지 1,2,3,5,6 검사
     * @return 새로 획득한 배지 목록
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<BadgeResponse> checkWalkEndBadges(Long userId) {
        List<Long> dogIds = getDogIds(userId);
        if (dogIds.isEmpty()) return Collections.emptyList();

        List<BadgeResponse> newBadges = new ArrayList<>();

        // 배지 1,2: 발자국 도장 수
        long footprintCount = footprintRepository.countByDogIds(dogIds);
        if (footprintCount >= 5) collectAwarded(newBadges, userId, PLACE_5);
        if (footprintCount >= 20) collectAwarded(newBadges, userId, PLACE_20);

        // 배지 3: 루트 있는 산책 수
        long walksWithRoute = walkRecordRepository.countWalksWithRoute(dogIds);
        if (walksWithRoute >= 5) collectAwarded(newBadges, userId, NEW_COURSE_5);

        // 배지 5: 사진+일기 있는 산책 수
        long walksWithPhotoAndDiary = walkRecordRepository.countWalksWithPhotoAndDiary(dogIds);
        if (walksWithPhotoAndDiary >= 10) collectAwarded(newBadges, userId, PHOTO_DIARY_10);

        // 배지 6: 총 사진 수
        long totalPhotos = walkRecordRepository.countTotalPhotos(dogIds);
        if (totalPhotos >= 10) collectAwarded(newBadges, userId, PHOTO_10);

        return newBadges;
    }

    /**
     * 위험 신고 후 호출 — 배지 4 검사
     * @return 새로 획득한 배지 목록
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<BadgeResponse> checkRiskReportBadges(Long userId) {
        List<BadgeResponse> newBadges = new ArrayList<>();
        long reportCount = riskReportRepository.countByUserId(userId);
        if (reportCount >= 3) collectAwarded(newBadges, userId, RISK_REPORT_3);
        return newBadges;
    }

    /**
     * 반려견 프로필 등록 후 호출 — 배지 7 검사
     * @return 새로 획득한 배지 목록
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<BadgeResponse> checkProfileBadges(Long userId) {
        List<BadgeResponse> newBadges = new ArrayList<>();
        collectAwarded(newBadges, userId, PROFILE_REGISTERED);
        return newBadges;
    }

    /**
     * 강아지 만남 후 호출 — 배지 8,9 검사
     * @return 새로 획득한 배지 목록
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<BadgeResponse> checkMeetBadges(Long userId) {
        List<Long> dogIds = getDogIds(userId);
        if (dogIds.isEmpty()) return Collections.emptyList();

        List<BadgeResponse> newBadges = new ArrayList<>();

        // 배지 8: 첫 만남
        boolean hasMet = metDogRepository.existsMeetByDogIds(dogIds);
        if (hasMet) collectAwarded(newBadges, userId, FIRST_MEET);

        // 배지 9: 5마리 이상 만남
        long metCount = metDogRepository.countMetDogsByDogIds(dogIds);
        if (metCount >= 5) collectAwarded(newBadges, userId, MEET_5_DOGS);

        return newBadges;
    }

    // ── 조회 API ──

    public List<BadgeResponse> getAllBadges() {
        return badgeRepository.findAll().stream()
                .map(BadgeResponse::from)
                .toList();
    }

    public List<UserBadgeResponse> getMyBadges(Long userId) {
        return userBadgeRepository.findAllByUserId(userId).stream()
                .map(UserBadgeResponse::from)
                .toList();
    }

    public List<BadgeProgressResponse> getBadgeProgress(Long userId) {
        List<Badge> allBadges = badgeRepository.findAll();
        List<Long> dogIds = getDogIds(userId);
        List<UserBadge> earnedBadges = userBadgeRepository.findAllByUserId(userId);

        // 현재 진행도 계산
        long footprintCount = dogIds.isEmpty() ? 0 : footprintRepository.countByDogIds(dogIds);
        long completedWalks = dogIds.isEmpty() ? 0 : walkRecordRepository.countCompletedWalksByDogIds(dogIds);
        long walksWithRoute = dogIds.isEmpty() ? 0 : walkRecordRepository.countWalksWithRoute(dogIds);
        long reportCount = riskReportRepository.countByUserId(userId);
        long walksWithPhotoAndDiary = dogIds.isEmpty() ? 0 : walkRecordRepository.countWalksWithPhotoAndDiary(dogIds);
        long totalPhotos = dogIds.isEmpty() ? 0 : walkRecordRepository.countTotalPhotos(dogIds);
        long dogCount = dogIds.size();
        boolean hasMet = dogIds.isEmpty() ? false : metDogRepository.existsMeetByDogIds(dogIds);
        long metDogCount = dogIds.isEmpty() ? 0 : metDogRepository.countMetDogsByDogIds(dogIds);

        List<BadgeProgressResponse> result = new ArrayList<>();
        for (Badge badge : allBadges) {
            int currentValue = getCurrentValue(badge.getBadgeName(),
                    footprintCount, completedWalks, walksWithRoute, reportCount,
                    walksWithPhotoAndDiary, totalPhotos, dogCount, hasMet, metDogCount);
            int targetValue = parseTarget(badge.getConditionValue());
            boolean earned = earnedBadges.stream()
                    .anyMatch(ub -> ub.getBadge().getId().equals(badge.getId()));

            result.add(BadgeProgressResponse.builder()
                    .badgeId(badge.getId())
                    .badgeName(badge.getBadgeName())
                    .description(badge.getDescription())
                    .currentValue(Math.min(currentValue, targetValue))
                    .targetValue(targetValue)
                    .earned(earned)
                    .build());
        }
        return result;
    }

    // ── private 헬퍼 ──

    private void collectAwarded(List<BadgeResponse> list, Long userId, String badgeName) {
        Badge badge = tryAward(userId, badgeName);
        if (badge != null) {
            list.add(BadgeResponse.from(badge));
        }
    }

    private Badge tryAward(Long userId, String badgeName) {
        Badge badge = badgeRepository.findByBadgeName(badgeName).orElse(null);
        if (badge == null) {
            log.warn("배지 '{}' 가 DB에 없습니다.", badgeName);
            return null;
        }
        if (userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())) {
            return null;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;

        userBadgeRepository.save(UserBadge.builder().user(user).badge(badge).build());
        log.info("배지 지급: userId={}, badge={}", userId, badgeName);
        return badge;
    }

    private List<Long> getDogIds(Long userId) {
        List<Dog> dogs = dogRepository.findAllByUser_Id(userId);
        if (dogs.isEmpty()) return Collections.emptyList();
        return dogs.stream().map(Dog::getId).toList();
    }

    private int getCurrentValue(String badgeName,
                                long footprintCount, long completedWalks, long walksWithRoute, long reportCount,
                                long walksWithPhotoAndDiary, long totalPhotos,
                                long dogCount, boolean hasMet, long metDogCount) {
        return switch (badgeName) {
            case PLACE_5, PLACE_20 -> (int) footprintCount;
            case NEW_COURSE_5 -> (int) walksWithRoute;
            case RISK_REPORT_3 -> (int) reportCount;
            case PHOTO_DIARY_10 -> (int) walksWithPhotoAndDiary;
            case PHOTO_10 -> (int) totalPhotos;
            case PROFILE_REGISTERED -> (int) dogCount;
            case FIRST_MEET -> hasMet ? 1 : 0;
            case MEET_5_DOGS -> (int) metDogCount;
            default -> 0;
        };
    }

    private int parseTarget(String conditionValue) {
        try {
            return Integer.parseInt(conditionValue);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
