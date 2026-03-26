package com.frontend.domain.usecase

import com.frontend.data.repository.DangerZoneRepository
import com.frontend.domain.model.PersistedDangerZone
import javax.inject.Inject

/**
 * 내 위험 구역 전체 목록 조회 UseCase
 * - 앱/화면 재진입 시 개인 위험장소 복원 용도
 * - GET /api/v1/safety/risk-zones/mine
 */
class GetMyRiskZonesUseCase @Inject constructor(
    private val dangerZoneRepository: DangerZoneRepository
) {
    suspend operator fun invoke(): Result<List<PersistedDangerZone>> =
        dangerZoneRepository.getMyRiskZones()
}
