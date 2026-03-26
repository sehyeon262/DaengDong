package com.frontend.domain.usecase

import com.frontend.data.repository.DangerZoneRepository
import com.frontend.domain.model.NearbyDangerZone
import javax.inject.Inject

/**
 * 내 주변 위험 구역 조회 UseCase (거리 포함)
 * - 산책 중 근접 알림 입력 용도
 * - GET /api/v1/safety/risk-zones/nearby
 */
class GetNearbyMyRiskZonesUseCase @Inject constructor(
    private val dangerZoneRepository: DangerZoneRepository
) {
    suspend operator fun invoke(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double = 200.0
    ): Result<List<NearbyDangerZone>> =
        dangerZoneRepository.getNearbyMyRiskZones(latitude, longitude, radiusMeters)
}
