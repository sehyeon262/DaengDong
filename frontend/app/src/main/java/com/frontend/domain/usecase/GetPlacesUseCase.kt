package com.frontend.domain.usecase

import com.frontend.data.repository.PlaceRepository
import com.frontend.domain.model.Place
import javax.inject.Inject

class GetPlacesUseCase @Inject constructor(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(
        latitude: Double,
        longitude: Double,
        radius: Double? = null,
        limit: Int? = null,
        category: String? = null
    ): Result<List<Place>> {
        return repository.getPlacesNearby(latitude, longitude, radius, limit, category)
    }
}
