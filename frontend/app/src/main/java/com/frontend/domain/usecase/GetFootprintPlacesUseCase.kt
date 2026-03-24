package com.frontend.domain.usecase

import com.frontend.data.repository.PlaceRepository
import com.frontend.domain.model.Place
import javax.inject.Inject

class GetFootprintPlacesUseCase @Inject constructor(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(dogId: Long): Result<List<Place>> {
        return repository.getFootprintPlaces(dogId)
    }
}
