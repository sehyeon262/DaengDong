package com.frontend.data.repository

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.DangerZoneApi
import com.frontend.domain.model.DangerLocation
import com.frontend.domain.model.DangerReason
import com.frontend.domain.model.DangerZone
import com.frontend.domain.model.ReportDangerZoneRequest
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DangerZoneRepository @Inject constructor(
    private val dangerZoneApi: DangerZoneApi,
    private val tokenDataStore: TokenDataStore
) {

    /**
     * 위험 구역 신고 API 호출.
     * - API 성공/실패 여부와 무관하게 DangerZone 객체를 반환해 지도 마커는 항상 표시됩니다.
     * - 백엔드 API 응답이 ApiResponse<Unit>이므로 DangerZone은 요청 파라미터로 구성합니다.
     * - id는 UI 마커 추적용 로컬 값입니다 (서버 ID 아님).
     */
    suspend fun reportDangerZone(
        walkId: Long?,
        location: DangerLocation,
        reason: DangerReason,
        customReason: String?
    ): Result<DangerZone> = runCatching {
        // API 호출 시도 (실패해도 로컬 마커는 표시)
        try {
            val token = tokenDataStore.getAccessToken().first()
            if (token != null) {
                dangerZoneApi.reportDangerZone(
                    authorization = "Bearer $token",
                    request = ReportDangerZoneRequest(
                        walkId = walkId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        reason = reason.name,
                        customReason = customReason
                    )
                )
            }
        } catch (e: Exception) {
            // API 실패 로그 (로컬 마커 표시는 계속 진행)
            android.util.Log.w("DangerZoneRepository", "API 신고 실패: ${e.message}")
        }

        // API 응답이 Unit이므로 요청 정보로 DangerZone 구성 (마커 표시용)
        DangerZone(
            id = System.currentTimeMillis(),
            location = location,
            reason = reason,
            customReason = customReason
        )
    }
}
