package com.frontend.domain.usecase

import com.frontend.data.repository.DangerZoneRepository
import com.frontend.domain.model.DangerZone
import javax.inject.Inject

class GetDangerZonesUseCase @Inject constructor(
    private val dangerZoneRepository: DangerZoneRepository
) {
    suspend operator fun invoke(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double = 5000.0
    ): Result<List<DangerZone>> = dangerZoneRepository.getDangerZones(latitude, longitude, radiusMeters)
}
