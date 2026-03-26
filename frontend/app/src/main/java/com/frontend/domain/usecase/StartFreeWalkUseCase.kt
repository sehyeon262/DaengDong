package com.frontend.domain.usecase

import com.frontend.data.repository.WalkRepository
import com.frontend.domain.model.StartWalkRequest
import javax.inject.Inject

/**
 * 산책 시작 UseCase
 *
 * 자유 산책과 추천 경로 산책 모두 처리.
 * - 자유 산책: request.selectedType = null
 * - 추천 경로: request.selectedType != null
 */
class StartFreeWalkUseCase @Inject constructor(
    private val repository: WalkRepository
) {
    suspend operator fun invoke(request: StartWalkRequest): Result<Long> {
        return repository.startWalk(request)
    }
}
