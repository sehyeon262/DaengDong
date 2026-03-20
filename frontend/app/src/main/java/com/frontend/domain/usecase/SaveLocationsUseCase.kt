package com.frontend.domain.usecase

import com.frontend.data.repository.WalkRepository
import com.frontend.domain.model.LocationBatchRequest
import javax.inject.Inject

class SaveLocationsUseCase @Inject constructor(
    private val repository: WalkRepository
) {
    suspend operator fun invoke(
        walkId: Long,
        points: List<LocationBatchRequest.LocationPoint>
    ): Result<Unit> {
        return repository.saveLocations(walkId, points)
    }
}
