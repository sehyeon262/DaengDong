package com.e108.be.domain.walk.controller;

import com.e108.be.domain.walk.dto.request.*;
import com.e108.be.domain.walk.dto.request.StartWalkRequest;
import com.e108.be.domain.walk.dto.response.EndWalkResponse;
import com.e108.be.domain.walk.dto.response.MetDogResponse;
import com.e108.be.domain.walk.dto.response.NearbyDogsResponse;
import com.e108.be.domain.walk.dto.response.ProposalResponse;
import com.e108.be.domain.walk.dto.response.StartWalkResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import com.e108.be.domain.walk.dto.response.WalkDurationResponse;
import com.e108.be.domain.walk.dto.response.CaloriesResponse;
import com.e108.be.domain.walk.dto.response.DistanceResponse;
import com.e108.be.domain.walk.dto.response.WalkDetailResponse;
import com.e108.be.domain.walk.service.WalkService;
import com.e108.be.global.common.template.ResTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * [Controller 패키지]
 * - 클라이언트(프론트엔드)의 HTTP 요청을 받는 창구
 * - 요청을 받아서 → Service에 위임
 * - 비즈니스 로직은 여기에 쓰지 않음
 *
 * 요청 흐름: 클라이언트 → Controller → Service → Repository → DB
 * 응답 흐름: DB → Repository → Service → Controller → 클라이언트
 */
@RestController
@RequestMapping("/walks")
@RequiredArgsConstructor
public class WalkController {

    private final WalkService walkService;

    /**
     * W1-01 산책 시작
     * POST /api/v1/walks
     *
     * 경로 추천 기반 산책: selectedType을 포함하여 요청
     * 자유 산책: selectedType을 null로 요청 (기존 호환)
     *
     * @param memberId JWT에서 추출한 회원 ID (경로 선택 로그 기록용)
     * @param request  산책 시작 정보 + 경로 선택 정보 (선택)
     */
    @PostMapping
    public ResTemplate<StartWalkResponse> startWalk(
            @AuthenticationPrincipal Long memberId,
            @RequestBody StartWalkRequest request) {
        StartWalkResponse response = walkService.startWalk(memberId, request);
        return ResTemplate.success(HttpStatus.OK, "산책이 시작되었습니다.", response);
    }

    /**
     * R1-03 자유 산책 시작
     * POST /api/v1/walks/free-start
     *
     * @deprecated POST /api/v1/walks 로 통합됨. selectedType을 null로 보내면 자유 산책.
     */
    @Deprecated
    @PostMapping("/free-start")
    public ResTemplate<StartWalkResponse> startFreeWalk(
            @AuthenticationPrincipal Long memberId,
            @RequestBody StartWalkRequest request) {
        StartWalkResponse response = walkService.startFreeWalk(memberId, request);
        return ResTemplate.success(HttpStatus.OK, "자유 산책이 시작되었습니다.", response);
    }

    @GetMapping("/{walkId}/distance")
    public ResTemplate<DistanceResponse> getDistance(@PathVariable Long walkId) {
        DistanceResponse response = walkService.getDistance(walkId);
        return ResTemplate.success(HttpStatus.OK, "거리 조회 성공", response);
    }

    /**
     * W1-03 산책 시간 조회
     * GET /api/v1/walks/{walkId}/duration
     * 소모 칼로리 조회
     * GET /api/v1/walks/{walkId}/calories
     *
     * 체중 있을 때:
     * { "code": 200, "message": "칼로리 조회 성공", "data": {"calories": 45.6, "requiresWeight": false} }
     *
     * 체중 미입력 시:
     * { "code": 200, "message": "체중을 입력해 주세요", "data": {"requiresWeight": true} }
     */
    @GetMapping("/{walkId}/duration")
    public ResTemplate<WalkDurationResponse> getWalkDuration(@PathVariable Long walkId) {
        WalkDurationResponse response = walkService.getWalkDuration(walkId);
        return ResTemplate.success(HttpStatus.OK, "산책 시간 조회 성공", response);
    }

    /**
     * W1-06 산책 종료
     * POST /api/v1/walks/{walkId}/end
     */
    @PostMapping("/{walkId}/end")
    public ResTemplate<EndWalkResponse> endWalk(@PathVariable Long walkId) {
        EndWalkResponse response = walkService.endWalk(walkId);
        return ResTemplate.success(HttpStatus.OK, "산책이 종료되었습니다.", response);
    }

    @GetMapping("/{walkId}/calories")
    public ResTemplate<CaloriesResponse> getCalories(@PathVariable Long walkId) {
        CaloriesResponse response = walkService.getCalories(walkId);
        if (response.isRequiresWeight()) {
            return ResTemplate.success(HttpStatus.OK, "체중을 입력해 주세요", response);
        }
        return ResTemplate.success(HttpStatus.OK, "칼로리 조회 성공", response);
    }

    /**
     * 산책 상세 조회 (산책 데이터 + 강아지 + 일기)
     * GET /api/v1/walks/{walkId}
     */
    @GetMapping("/{walkId}")
    public ResTemplate<WalkDetailResponse> getWalkDetail(@PathVariable Long walkId) {
        WalkDetailResponse response = walkService.getWalkDetail(walkId);
        return ResTemplate.success(HttpStatus.OK, "산책 상세 조회 성공", response);
    }

    /**
     * 산책 사진 업로드
     * POST /api/v1/walks/{walkId}/photos
     */
    @PostMapping("/{walkId}/photos")
    public ResTemplate<List<String>> uploadPhotos(
            @PathVariable Long walkId,
            @RequestParam("files") List<MultipartFile> files) {
        List<String> urls = walkService.uploadPhotos(walkId, files);
        return ResTemplate.success(HttpStatus.OK, "사진 업로드 성공", urls);
    }

    /**
     * 산책 사진 삭제
     * DELETE /api/v1/walks/{walkId}/photos
     */
    @DeleteMapping("/{walkId}/photos")
    public ResTemplate<List<String>> deletePhoto(
            @PathVariable Long walkId,
            @RequestBody DeletePhotoRequest request) {
        List<String> urls = walkService.deletePhoto(walkId, request.getPhotoUrl());
        return ResTemplate.success(HttpStatus.OK, "사진 삭제 성공", urls);
    }

    /**
     * S14P21E108-165: 산책 중 주변 반려견 조회
     * GET /api/v1/walks/nearby-dogs?lat=&lon=&radius=&myDogId=&myWalkRecordId=
     */
    @GetMapping("/nearby-dogs")
    public ResTemplate<NearbyDogsResponse> getNearbyDogs(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "500") double radius,
            @RequestParam Long myDogId,
            @RequestParam(required = false) Long myWalkRecordId) {
        NearbyDogsResponse response = walkService.getNearbyDogs(lat, lon, radius, myDogId, myWalkRecordId);
        return ResTemplate.success(HttpStatus.OK, "주변 강아지 조회 성공", response);
    }

    /**
     * S14P21E108-172: 산책 제안 전송
     * POST /api/v1/walks/proposals
     */
    @PostMapping("/proposals")
    public ResTemplate<ProposalResponse> sendProposal(@RequestBody ProposalRequest request) {
        ProposalResponse response = walkService.sendProposal(request);
        return ResTemplate.success(HttpStatus.OK, "산책 제안이 전송되었습니다.", response);
    }

    /**
     * S14P21E108-172: 산책 제안 수락/거절
     * PATCH /api/v1/walks/proposals/{proposalId}
     */
    @PatchMapping("/proposals/{proposalId}")
    public ResTemplate<java.util.Map<String, Long>> respondToProposal(
            @PathVariable String proposalId,
            @RequestBody ProposalRespondRequest request) {
        Long chatRoomId = walkService.respondToProposal(proposalId, request);
        java.util.Map<String, Long> data = chatRoomId != null
                ? java.util.Map.of("chatRoomId", chatRoomId)
                : java.util.Collections.emptyMap();
        return ResTemplate.success(HttpStatus.OK, "산책 제안 처리 완료", data);
    }

    /**
     * S14P21E108-172: 산책 종료 시 2m 자동 만남 일괄 기록
     * POST /api/v1/walks/{walkRecordId}/encounters
     */
    @PostMapping("/{walkRecordId}/encounters")
    public ResTemplate<Void> recordEncounters(
            @PathVariable Long walkRecordId,
            @RequestBody EncounterRequest request) {
        walkService.recordEncounters(walkRecordId, request);
        return ResTemplate.success(HttpStatus.OK, "만남이 기록되었습니다.", null);
    }

    /**
     * 만난 친구 목록 조회
     * GET /api/v1/walks/met-dogs?dogId=
     */
    @GetMapping("/met-dogs")
    public ResTemplate<List<MetDogResponse>> getMetDogs(@RequestParam Long dogId) {
        List<MetDogResponse> response = walkService.getMetDogs(dogId);
        return ResTemplate.success(HttpStatus.OK, "만난 친구 목록 조회 성공", response);
    }

    /**
     * S14P21E108-172: 피드백 설정/수정
     * PATCH /api/v1/walks/met-dogs/feedback
     */
    @PatchMapping("/met-dogs/feedback")
    public ResTemplate<Void> updateFeedback(@RequestBody FeedbackRequest request) {
        walkService.updateFeedback(request);
        return ResTemplate.success(HttpStatus.OK, "피드백이 저장되었습니다.", null);
    }
}
