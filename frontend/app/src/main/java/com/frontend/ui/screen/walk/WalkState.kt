package com.frontend.ui.screen.walk

import com.frontend.domain.model.DangerLocation
import com.frontend.domain.model.DangerReason
import com.frontend.domain.model.DangerZone

data class WalkState(
    val selectedRouteIndex: Int = 0,
    val showFilterSheet: Boolean = false,
    val selectedFilter: WalkFilterType = WalkFilterType.PLACE,

    // ── 위험 구역 신고 ────────────────────────────────────────────────────────
    val isSelectingDangerZone: Boolean = false,       // 위치 선택 모드 여부
    val selectedLocation: DangerLocation? = null,     // 선택된 좌표 (지도 중심)
    val isDangerReportDialogOpen: Boolean = false,    // 신고 모달 열림 여부
    val selectedDangerReason: DangerReason? = null,   // 선택된 위험 사유
    val customDangerReason: String = "",              // "기타" 직접 입력 텍스트
    val dangerZones: List<DangerZone> = emptyList(),  // 신고 완료된 위험 구역 목록
    val isSubmitting: Boolean = false                 // 제출 중 여부
)
