package com.frontend.domain.usecase

import com.frontend.data.repository.DangerZoneRepository
import javax.inject.Inject

class DeleteMyRiskZoneUseCase @Inject constructor(
    private val dangerZoneRepository: DangerZoneRepository
) {
    suspend operator fun invoke(riskReportId: Long): Result<Unit> =
        dangerZoneRepository.deleteMyRiskZone(riskReportId)
}
