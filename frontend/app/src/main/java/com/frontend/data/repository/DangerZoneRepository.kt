package com.frontend.data.repository

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.DangerZoneApi
import com.frontend.domain.model.DangerLocation
import com.frontend.domain.model.DangerReason
import com.frontend.domain.model.DangerZone
import com.frontend.domain.model.DangerZoneResult
import com.frontend.domain.model.NewBadgeInfo
import com.frontend.domain.model.ReportDangerZoneRequest
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DangerZoneRepository @Inject constructor(
    private val dangerZoneApi: DangerZoneApi,
    private val tokenDataStore: TokenDataStore
) {

    /**
     * 위험 구역 신고 API 호출.
     * - reason + customReason을 합쳐 백엔드 description 필드로 전송합니다.
     * - walkId → walkSessionId 로 필드명 수정.
     * - API 성공 시 서버 ID(riskReportId)를 DangerZone.id로 사용합니다.
     * - API 실패 시에도 로컬 마커는 표시됩니다.
     */
    suspend fun reportDangerZone(
        walkId: Long?,
        location: DangerLocation,
        reason: DangerReason,
        customReason: String?
    ): Result<DangerZoneResult> = runCatching {
        val description = if (customReason != null) {
            "${reason.label}: $customReason"
        } else {
            reason.label
        }

        var newBadges: List<NewBadgeInfo> = emptyList()
        var serverId: Long? = null

        try {
            val token = tokenDataStore.getAccessToken().first()
            if (token != null) {
                val response = dangerZoneApi.reportDangerZone(
                    authorization = "Bearer $token",
                    request = ReportDangerZoneRequest(
                        walkSessionId = walkId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        description = description
                    )
                )
                serverId = response.data?.riskReportId
                newBadges = response.data?.newBadges ?: emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.w("DangerZoneRepository", "API 신고 실패: ${e.message}")
        }

        DangerZoneResult(
            dangerZone = DangerZone(
                id = serverId ?: System.currentTimeMillis(),
                location = location,
                reason = reason,
                customReason = customReason
            ),
            newBadges = newBadges
        )
    }

    /**
     * 현재 위치 주변 위험 구역 목록 조회.
     * 백엔드에서 서버 DB에 저장된 모든 위험 구역을 반환합니다.
     */
    suspend fun getDangerZones(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double = 5000.0
    ): Result<List<DangerZone>> = runCatching {
        val token = tokenDataStore.getAccessToken().first()
            ?: error("로그인이 필요합니다.")

        val response = dangerZoneApi.getDangerZones(
            authorization = "Bearer $token",
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters
        )

        (response.data ?: emptyList()).map { data ->
            val (parsedReason, parsedCustomReason) = parseDescription(data.description)
            DangerZone(
                id = data.riskReportId,
                location = DangerLocation(data.latitude, data.longitude),
                reason = parsedReason,
                customReason = parsedCustomReason
            )
        }
    }

    /**
     * 저장된 description 문자열에서 DangerReason과 customReason을 복원합니다.
     * 포맷: "${reason.label}: $customReason" 또는 "${reason.label}"
     */
    private fun parseDescription(description: String): Pair<DangerReason, String?> {
        for (reason in DangerReason.entries) {
            if (description.startsWith("${reason.label}: ")) {
                val custom = description.removePrefix("${reason.label}: ").takeIf { it.isNotBlank() }
                return Pair(reason, custom)
            }
            if (description == reason.label) {
                return Pair(reason, null)
            }
        }
        return Pair(DangerReason.OTHER, description)
    }
}
