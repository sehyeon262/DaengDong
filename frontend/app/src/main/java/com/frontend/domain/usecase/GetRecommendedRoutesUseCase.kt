package com.frontend.domain.usecase

import com.frontend.data.repository.RouteRepository
import com.frontend.domain.model.RouteRecommendResponse
import javax.inject.Inject

class GetRecommendedRoutesUseCase @Inject constructor(
    private val routeRepository: RouteRepository
) {
    /**
     * 현재 위치 기반 추천 경로 조회
     */
    suspend operator fun invoke(
        latitude: Double,
        longitude: Double
    ): Result<RouteRecommendResponse> {
        return routeRepository.getRecommendedRoutes(latitude, longitude)
    }
}
