package com.frontend.domain.usecase

import com.frontend.data.repository.PlaceRepository
import javax.inject.Inject

class CancelStampUseCase @Inject constructor(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(dogId: Long, placeId: Long): Result<Unit> {
        return repository.cancelStamp(dogId, placeId)
    }
}
