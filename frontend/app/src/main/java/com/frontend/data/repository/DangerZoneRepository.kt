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

    suspend fun reportDangerZone(
        walkId: Long?,
        location: DangerLocation,
        reason: DangerReason,
        customReason: String?
    ): Result<DangerZoneResult> = runCatching {
        var newBadges: List<NewBadgeInfo> = emptyList()

        try {
            val token = tokenDataStore.getAccessToken().first()
            if (token != null) {
                val response = dangerZoneApi.reportDangerZone(
                    authorization = "Bearer $token",
                    request = ReportDangerZoneRequest(
                        walkId = walkId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        reason = reason.name,
                        customReason = customReason
                    )
                )
                newBadges = response.data?.newBadges ?: emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.w("DangerZoneRepository", "API 신고 실패: ${e.message}")
        }

        DangerZoneResult(
            dangerZone = DangerZone(
                id = System.currentTimeMillis(),
                location = location,
                reason = reason,
                customReason = customReason
            ),
            newBadges = newBadges
        )
    }
}
