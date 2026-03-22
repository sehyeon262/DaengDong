package com.frontend.domain.usecase

import com.frontend.data.repository.PlaceRepository
import com.frontend.domain.model.Place
import javax.inject.Inject

class GetPlaceDetailUseCase @Inject constructor(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(placeId: Long): Result<Place> {
        return repository.getPlaceDetail(placeId)
    }
}
