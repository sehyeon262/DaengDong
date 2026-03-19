package com.frontend.ui.screen.walk

import com.frontend.domain.model.DangerLocation
import com.frontend.domain.model.DangerReason
import com.frontend.domain.model.DangerZone
import com.frontend.domain.model.Place

data class WalkState(
    val selectedRouteIndex: Int = 0,
    val showFilterSheet: Boolean = false,

    // 다중 선택 필터 (각 항목을 독립적으로 on/off)
    val activeFilters: Set<WalkFilterType> = emptySet(),
    // 바텀시트에서 임시로 편집 중인 필터 상태 (적용하기 전)
    val pendingFilters: Set<WalkFilterType> = emptySet(),

    // ── 장소 마커 ──────────────────────────────────────────────────────────────
    val places: List<Place> = emptyList(),            // 지도에 표시할 장소 목록
    val isPlacesLoading: Boolean = false,             // 장소 로딩 중 여부

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
