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

    suspend fun reportDangerZone(
        walkId: Long?,
        location: DangerLocation,
        reason: DangerReason,
        customReason: String?
    ): Result<DangerZone> = runCatching {

        // ── [더미] 실제 API 연동 전 로컬에서 즉시 생성 ──────────────────────────
        DangerZone(
            id = System.currentTimeMillis(),
            location = location,
            reason = reason,
            customReason = customReason
        )

        // ── [실제 API] 아래 주석 해제 후 더미 블록 삭제 ──────────────────────────
        // val token = tokenDataStore.getAccessToken().first()
        //     ?: throw Exception("로그인이 필요합니다")
        // dangerZoneApi.reportDangerZone(
        //     authorization = "Bearer $token",
        //     request = ReportDangerZoneRequest(
        //         walkId = walkId,
        //         latitude = location.latitude,
        //         longitude = location.longitude,
        //         reason = reason.name,
        //         customReason = customReason
        //     )
        // )
        // DangerZone(
        //     id = System.currentTimeMillis(),
        //     location = location,
        //     reason = reason,
        //     customReason = customReason
        // )
    }
}
