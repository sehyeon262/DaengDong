package com.frontend.ui.screen.walk

import com.frontend.domain.model.DangerLocation
import com.frontend.domain.model.DangerReason
import com.frontend.domain.model.DangerZone
import com.frontend.domain.model.AcceptedProposalInfo
import com.frontend.domain.model.DogProfileResponse
import com.frontend.domain.model.NearbyDogResponse
import com.frontend.domain.model.PendingProposalInfo
import com.frontend.domain.model.Place
import com.frontend.domain.model.RecommendedRoute

data class WalkState(
    val selectedRouteIndex: Int = 0,
    val showFilterSheet: Boolean = false,

    // ── 추천 경로 ────────────────────────────────────────────────────────────
    val recommendedRoutes: List<RecommendedRoute> = emptyList(),  // API에서 받은 추천 경로 목록
    val isRoutesLoading: Boolean = false,                          // 경로 로딩 중 여부
    val routesError: String? = null,                               // 경로 로딩 에러
    val fallbackLevel: String? = null,                             // NORMAL, REDUCED, WALK_ONLY
    val fallbackMessage: String? = null,                           // 폴백 안내 메시지
    val showRecommendedRoute: Boolean = true,                      // 추천 경로 표시 여부 (토글)

    // 다중 선택 필터 (각 항목을 독립적으로 on/off)
    val activeFilters: Set<WalkFilterType> = emptySet(),
    // 바텀시트에서 임시로 편집 중인 필터 상태 (적용하기 전)
    val pendingFilters: Set<WalkFilterType> = emptySet(),

    // ── 장소 마커 ──────────────────────────────────────────────────────────────
    val places: List<Place> = emptyList(),            // 지도에 표시할 장소 목록
    val isPlacesLoading: Boolean = false,             // 장소 로딩 중 여부
    val selectedPlace: Place? = null,                 // 클릭된 장소 (상세 바텀시트 표시용)

    // ── 자유 산책 ──────────────────────────────────────────────────────────────
    val isWalking: Boolean = false,       // 산책 진행 중 여부
    val isPaused: Boolean = false,        // 일시정지 여부
    val elapsedSeconds: Int = 0,          // 경과 시간 (초)
    val distanceMeters: Double = 0.0,     // 누적 이동 거리 (미터)
    val walkError: String? = null,        // 산책 시작/종료 에러 메시지

    // ── 산책 요약 (종료 후 표시) ──────────────────────────────────────────────
    val isWalkSummaryVisible: Boolean = false,
    val summaryElapsedSeconds: Int = 0,
    val summaryDistanceMeters: Double = 0.0,
    val summaryRouteName: String = "",
    val summaryRating: Int = 0,           // 0 = 미평가, 1~5 = 별점

    // ── 소셜 산책 상태 ────────────────────────────────────────────────────────
    val currentWalkId: Long? = null,                  // 현재 산책 레코드 ID
    val myDogId: Long? = null,                        // 내 강아지 ID

    // ── 주변 강아지 ───────────────────────────────────────────────────────────
    val nearbyDogs: List<NearbyDogResponse> = emptyList(), // 주변 강아지 목록

    // ── 강아지 공개 프로필 팝업 ────────────────────────────────────────────────
    val selectedNearbyDog: NearbyDogResponse? = null,      // 마커 클릭된 강아지
    val dogPublicProfile: DogProfileResponse? = null,      // 공개 프로필 응답
    val isDogProfileLoading: Boolean = false,

    // ── 함께 산책 제안 ─────────────────────────────────────────────────────────
    val isSendingProposal: Boolean = false,                 // 제안 전송 중 여부
    val proposalSentDogId: Long? = null,                   // 제안 보낸 강아지 ID (버튼 상태용)
    val pendingProposals: List<PendingProposalInfo> = emptyList(),   // 받은 제안 목록
    val acceptedProposals: List<AcceptedProposalInfo> = emptyList(), // 수락된 제안 알림

    // ── 위험 구역 신고 ────────────────────────────────────────────────────────
    val isSelectingDangerZone: Boolean = false,       // 위치 선택 모드 여부
    val selectedLocation: DangerLocation? = null,     // 선택된 좌표 (지도 중심)
    val isDangerReportDialogOpen: Boolean = false,    // 신고 모달 열림 여부
    val selectedDangerReason: DangerReason? = null,   // 선택된 위험 사유
    val customDangerReason: String = "",              // "기타" 직접 입력 텍스트
    val dangerZones: List<DangerZone> = emptyList(),  // 신고 완료된 위험 구역 목록
    val isLoading: Boolean = false,                   // 제출 중 여부
    val error: String? = null                         // 에러 메시지 (없으면 null)
)
