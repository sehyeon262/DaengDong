package com.frontend.domain.usecase

import com.frontend.data.repository.PlaceRepository
import com.frontend.domain.model.Place
import javax.inject.Inject

class GetPlacesUseCase @Inject constructor(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(latitude: Double, longitude: Double): Result<List<Place>> {
        return repository.getPlacesNearby(latitude, longitude)
    }
}
