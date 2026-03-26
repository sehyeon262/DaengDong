package com.frontend.domain.usecase

import com.frontend.data.repository.PlaceRepository
import javax.inject.Inject

class SendStampUseCase @Inject constructor(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(walkId: Long, dogId: Long, placeId: Long): Result<Unit> {
        return repository.sendStamp(walkId, dogId, placeId)
    }
}
