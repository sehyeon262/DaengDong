package com.frontend.domain.usecase

import com.frontend.data.repository.PlaceRepository
import com.frontend.domain.model.PlaceCategory
import javax.inject.Inject

class GetPlaceCategoriesUseCase @Inject constructor(
    private val repository: PlaceRepository
) {
    suspend operator fun invoke(): Result<List<PlaceCategory>> {
        return repository.getPlaceCategories()
    }
}
