package com.frontend.data.repository

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.DangerZoneApi
import com.frontend.domain.model.DangerLocation
import com.frontend.domain.model.DangerReason
import com.frontend.domain.model.DangerZone
import com.frontend.domain.model.DangerZoneResult
import com.frontend.domain.model.NearbyDangerZone
import com.frontend.domain.model.PersistedDangerZone
import com.frontend.domain.model.ReportDangerZoneRequest
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DangerZoneRepository @Inject constructor(
    private val dangerZoneApi: DangerZoneApi,
    private val tokenDataStore: TokenDataStore
) {

    /**
     * 위험 구역 신고 API 호출.
     * - 서버 실패 시 Result.failure 반환 (실패를 삼키지 않음)
     * - 성공 시 서버 ID(riskReportId) 기반 DangerZoneResult 반환
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

        val token = tokenDataStore.getAccessToken().first()
            ?: error("로그인이 필요합니다.")

        val response = dangerZoneApi.reportDangerZone(
            authorization = "Bearer $token",
            request = ReportDangerZoneRequest(
                walkSessionId = walkId,
                latitude = location.latitude,
                longitude = location.longitude,
                description = description
            )
        )

        val serverId = response.data?.riskReportId
            ?: error("서버 응답에 riskReportId가 없습니다.")
        val newBadges = response.data?.newBadges ?: emptyList()

        DangerZoneResult(
            dangerZone = DangerZone(
                id = serverId,
                location = location,
                reason = reason,
                customReason = customReason
            ),
            newBadges = newBadges
        )
    }

    /**
     * 현재 위치 주변 위험 구역 목록 조회 (전체 사용자).
     * - 지도 표시용 (알림용 아님)
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
     * 내 위험 구역 전체 목록 조회 (영구 저장 목록).
     * - 앱/화면 재진입 시 복원 용도
     * - GET /api/v1/safety/risk-zones/mine
     */
    suspend fun getMyRiskZones(): Result<List<PersistedDangerZone>> = runCatching {
        val token = tokenDataStore.getAccessToken().first()
            ?: error("로그인이 필요합니다.")

        val response = dangerZoneApi.getMyRiskZones(
            authorization = "Bearer $token"
        )

        (response.data ?: emptyList()).map { data ->
            val (parsedReason, parsedCustomReason) = parseDescription(data.description)
            PersistedDangerZone(
                id = data.riskReportId,
                location = DangerLocation(data.latitude, data.longitude),
                reason = parsedReason,
                customReason = parsedCustomReason,
                createdAt = data.createdAt
            )
        }
    }

    /**
     * 내 주변 위험 구역 조회 (거리 포함).
     * - 산책 중 근접 알림 입력 용도
     * - GET /api/v1/safety/risk-zones/nearby
     */
    suspend fun getNearbyMyRiskZones(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double = 200.0
    ): Result<List<NearbyDangerZone>> = runCatching {
        val token = tokenDataStore.getAccessToken().first()
            ?: error("로그인이 필요합니다.")

        val response = dangerZoneApi.getNearbyMyRiskZones(
            authorization = "Bearer $token",
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters
        )

        (response.data ?: emptyList()).map { data ->
            val (parsedReason, parsedCustomReason) = parseDescription(data.description)
            NearbyDangerZone(
                id = data.riskReportId,
                location = DangerLocation(data.latitude, data.longitude),
                reason = parsedReason,
                customReason = parsedCustomReason,
                distanceM = data.distanceM
            )
        }
    }

    /**
     * 저장된 description 문자열에서 DangerReason과 customReason을 복원합니다.
     * 포맷: "${reason.label}: $customReason" 또는 "${reason.label}"
     */
    suspend fun deleteMyRiskZone(riskReportId: Long): Result<Unit> = runCatching {
        val token = tokenDataStore.getAccessToken().first()
            ?: error("æ¿¡ì’“ë ‡?ëª„ì”  ?ê¾©ìŠ‚?â‘¸ë•²??")

        val response = dangerZoneApi.deleteMyRiskZone(
            authorization = "Bearer $token",
            riskReportId = riskReportId
        )

        if (response.code !in 200..299) {
            throw Exception(response.message)
        }
    }

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
