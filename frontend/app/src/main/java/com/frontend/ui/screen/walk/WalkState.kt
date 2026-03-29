package com.frontend.ui.screen.walk

import com.frontend.domain.model.ChatBannerNotification
import com.frontend.domain.model.DangerLocation
import com.frontend.domain.model.DangerReason
import com.frontend.domain.model.DangerZone
import com.frontend.domain.model.AcceptedProposalInfo
import com.frontend.domain.model.DogProfileResponse
import com.frontend.domain.model.NearbyDangerZone
import com.frontend.domain.model.NearbyDogResponse
import com.frontend.domain.model.NewBadgeInfo
import com.frontend.domain.model.PendingProposalInfo
import com.frontend.domain.model.PersistedDangerZone
import com.frontend.domain.model.RejectedProposalInfo
import com.frontend.domain.model.Place
import com.frontend.domain.model.RecommendedRoute

/**
 * 비선호 강아지 알림 상태 추적용 데이터 클래스
 * - 진입/이탈/쿨다운 기반으로 중복 알림 방지
 */
data class DogAlertState(
    val lastAlertTimeMs: Long = 0L,         // 마지막 알림 발송 시간
    val isInsideAlertRadius: Boolean = false, // 현재 알림 반경(50m) 안에 있는지
)

/**
 * 장소 발자국 알림 상태 추적용 데이터 클래스
 * - 20m 반경 진입/이탈 기반으로 알림 제어
 */
data class FootprintAlertState(
    val isInsideRadius: Boolean = false,  // 현재 20m 반경 안에 있는지
    val hasStamped: Boolean = false,      // 이번 산책에서 이미 도장 찍었는지 (영구 무시)
)

/**
 * 위험장소 알림 상태 추적용 데이터 클래스
 * - 진입 40m / 이탈 60m / 쿨다운 3분 기반 중복 알림 방지
 */
data class RiskZoneAlertState(
    val lastAlertTimeMs: Long = 0L,          // 마지막 알림 발송 시간
    val isInsideAlertRadius: Boolean = false, // 현재 알림 반경(40m) 안에 있는지
)

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

    // ── 발자국 마커 ────────────────────────────────────────────────────────────
    val footprintPlaces: List<Place> = emptyList(),   // 도장 찍은 장소 목록
    val isFootprintPlacesLoading: Boolean = false,    // 발자국 장소 로딩 중 여부
    val nearbyStampablePlace: Place? = null,          // 50m 이내 도장 찍을 수 있는 장소
    val stampedPlaceIds: Set<Long> = emptySet(),      // 이번 산책에서 도장 찍은 장소 ID
    val cancelStampPlace: Place? = null,              // 취소 확인 다이얼로그 표시용 장소 (null=숨김)

    // ── 발자국 찍기 오버레이 ────────────────────────────────────────────────
    val footprintAlertPlace: Place? = null,                        // 현재 20m 이내의 장소 (null=오버레이 없음)
    val footprintStamped: Boolean = false,                          // 도장 찍기 완료 여부
    val footprintAlertStates: Map<Long, FootprintAlertState> = emptyMap(), // 장소별 진입/이탈/도장 상태
    val walkPlaces: List<Place> = emptyList(),                      // 발자국 감지용 주변 장소 목록

    // ── 자유 산책 ──────────────────────────────────────────────────────────────
    val isWalking: Boolean = false,       // 산책 진행 중 여부
    val isPaused: Boolean = false,        // 일시정지 여부
    val elapsedSeconds: Int = 0,          // 경과 시간 (초)
    val distanceMeters: Double = 0.0,     // 누적 이동 거리 (미터)
    val walkError: String? = null,        // 산책 시작/종료 에러 메시지

    // ── 산책 요약 (종료 후 표시) ──────────────────────────────────────────────
    val isWalkSummaryVisible: Boolean = false,
    val summaryWalkId: Long? = null,      // 종료된 산책 ID (일기 보러가기용)
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
    val acceptedProposals: List<AcceptedProposalInfo> = emptyList(), // 수락된 제안 알림 (제안자용 polling)
    val rejectedProposals: List<RejectedProposalInfo> = emptyList(), // 거절된 제안 알림 (제안자용 polling)
    val showAcceptedByMeDialog: Boolean = false,  // 수락자 확인 모달 ("함께 산책하기를 수락했습니다")
    val showRejectedByMeDialog: Boolean = false,  // 거절자 확인 모달 ("산책 거절 메시지를 보냈습니다")

    // ── 비선호 강아지 경고 (S14P21E108-175) ─────────────────────────────────
    val warningDog: NearbyDogResponse? = null,             // 현재 경고 표시 중인 비선호 강아지
    val dogAlertStates: Map<Long, DogAlertState> = emptyMap(),  // 진입/이탈/쿨다운 기반 알림 상태

    // ── 위험 구역 신고 ────────────────────────────────────────────────────────
    val isSelectingDangerZone: Boolean = false,       // 위치 선택 모드 여부
    val selectedLocation: DangerLocation? = null,     // 선택된 좌표 (지도 중심)
    val isDangerReportDialogOpen: Boolean = false,    // 신고 모달 열림 여부
    val selectedDangerReason: DangerReason? = null,   // 선택된 위험 사유
    val customDangerReason: String = "",              // "기타" 직접 입력 텍스트
    val dangerZones: List<DangerZone> = emptyList(),  // (레거시) 세션 중 신고된 위험 구역 임시 목록
    val isLoading: Boolean = false,                   // 제출 중 여부
    val error: String? = null,                        // 에러 메시지 (없으면 null)

    // ── 개인 위험장소 (영구저장/근접알림) ──────────────────────────────────────
    val persistedDangerZones: List<PersistedDangerZone> = emptyList(),  // 서버 영구저장 목록 (mine API)
    val nearbyDangerZones: List<NearbyDangerZone> = emptyList(),        // 산책 중 반경 조회 결과 (nearby API)
    val riskZoneAlertStates: Map<Long, RiskZoneAlertState> = emptyMap(), // 위험장소별 알림 상태
    val warningRiskZoneQueue: List<NearbyDangerZone> = emptyList(),      // 경고 대기열 (큐 방식, 첫 번째가 현재 표시)

    // ── 배지 획득 알림 ──────────────────────────────────────────────────────
    val newBadges: List<NewBadgeInfo> = emptyList(),   // 새로 획득한 배지 목록

    // ── 채팅 관련 상태 ──────────────────────────────────────────────────────
    /** dogId → chatRoomId: 수락된 제안의 채팅방 정보 보관 (프로필 팝업에서 채팅 버튼 표시용) */
    val acceptedChatRooms: Map<Long, Long> = emptyMap(),
    /** 수락자 확인 다이얼로그에서 바로 채팅방 이동할 수 있도록 최근 수락한 chatRoomId 보관 */
    val acceptedByMeChatRoomId: Long? = null,
    /** 화면 상단에 표시할 채팅 배너 알림 (null이면 숨김) */
    val chatBanner: ChatBannerNotification? = null,
)
