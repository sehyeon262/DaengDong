package com.frontend.domain.usecase

import com.frontend.data.repository.DangerZoneRepository
import com.frontend.domain.model.DangerLocation
import com.frontend.domain.model.DangerReason
import com.frontend.domain.model.DangerZone
import javax.inject.Inject

class ReportDangerZoneUseCase @Inject constructor(
    private val dangerZoneRepository: DangerZoneRepository
) {
    suspend operator fun invoke(
        walkId: Long?,
        location: DangerLocation,
        reason: DangerReason,
        customReason: String?
    ): Result<DangerZone> = dangerZoneRepository.reportDangerZone(walkId, location, reason, customReason)
}
